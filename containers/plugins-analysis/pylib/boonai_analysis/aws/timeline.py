import json
import logging

from boonsdk.entity import TimelineBuilder
from boonflow import file_storage
from boonflow.video import save_timeline, WebvttBuilder

logger = logging.getLogger(__name__)

__all__ = [
    'save_transcribe_webvtt',
    'save_transcribe_timeline',
    'save_raw_transcribe_result',
    'generate_transcribe_sentences'
]


def save_raw_transcribe_result(asset, audio_result):
    """
    Save the raw AWS Transcribe result.

    Args:
        asset (Asset): The asset to save to,.
        audio_result (dict): The transcribe result.

    Returns:
        dict: A StoredFile dictionary.

    """
    jstr = json.dumps(audio_result)
    return file_storage.assets.store_blob(jstr.encode(),
                                          asset,
                                          'aws',
                                          'aws-transcribe.json')


def save_transcribe_webvtt(asset, audio_result):
    """
    Save a baked out webvtt file.

    Args:
        asset (Asset): The asset to save to,.
        audio_result (dict): The transcribe result.

    Returns:
        tuple: with the file path and the StoredFile record.
    """
    with WebvttBuilder() as webvtt:
        for segment in generate_transcribe_sentences(audio_result):
            webvtt.append(segment['start_time'],
                          segment['end_time'],
                          segment['sentence'])

    logger.info(f'Saving speech-to-text data from {webvtt.path}')
    sf = file_storage.assets.store_file(webvtt.path, asset,
                                        'captions',
                                        'aws-transcribe.vtt')
    return webvtt.path, sf


def save_transcribe_timeline(asset, audio_result, default_lang):
    """
    Save the results of Transcribe to a timeline.

    Args:
        asset (Asset): The asset to register the file to.
        audio_result (obj): The speech to text result.
        default_lang (str): Tbe default language if no language is set on the result, which
            will be the case if only 1 language is passed to Transcribe.

    Returns:
        Timeline: The generated timeline.
    """
    timeline = TimelineBuilder(asset, 'aws-transcribe', replace=True)
    results = audio_result['results']
    track = 'Language {}'.format(results.get('language_code', default_lang))

    for segment in generate_transcribe_sentences(audio_result):
        timeline.add_clip(
            track,
            segment['start_time'],
            segment['end_time'],
            segment['sentence'],
            segment['confidence'])

    save_timeline(asset, timeline)
    return timeline


def generate_transcribe_sentences(audio_result):
    """
    Convert a AWS transcribe audio result into sentences.

    Args:
        audio_result (dict): A Transcribe audio result.

    Returns:
        Generator: Yields dictionaries with sentence data
    """
    def create_new_sentence(words, confidence, start, stop):
        return {
            'sentence': [words],
            'confidence': confidence,
            'start_time': start,
            'end_time': stop
        }

    new_sentence = True
    cursent = None
    items = audio_result["results"]["items"]

    for item in items:

        itype = item['type']
        content = item["alternatives"][0]["content"]

        if new_sentence:
            if 'start_time' not in item:
                # The sentence is starting without a start time
                # which is crazy
                continue

            cursent = create_new_sentence(content,
                                          float(item['alternatives'][0]['confidence']),
                                          float(item['start_time']),
                                          float(item['end_time']))
            new_sentence = False

        else:

            # In this block we're in an existing sentence, so we either
            # append the content or start a new sentence.

            if itype == 'punctuation' and content != ',':
                cursent['sentence'] = ' '.join(cursent['sentence']) + content
                cursent['sentence'] = cursent['sentence'].replace(' , ', ', ')
                yield cursent

                new_sentence = True
                cursent = None
            else:
                cursent['sentence'].append(content)
                if 'end_time' in item:
                    cursent['end_time'] = float(item['end_time'])
                    # Were taking lowest confidence
                    cursent['confidence'] = min(cursent['confidence'],
                                                float(item['alternatives'][0]['confidence']))

    if cursent:
        yield cursent
