import logging

from zmlp.entity import TimelineBuilder
from zmlpsdk.storage import file_storage
from zmlpsdk.video import WebvttBuilder, save_timeline

logger = logging.getLogger(__name__)


def save_text_detection_timeline(asset, annotations):
    """
    Build a timeline for video text detection.

    Args:
        asset (Asset): The Asset or Asset Id.
        annotations (AnnotateVideoResponse): The Video Intelligence response.

    Returns:
        TimelineBuilder: The populated TimelineBuilder.

    """
    timeline = TimelineBuilder(asset, 'gcp-video-text-detection')
    for annotation in annotations.text_annotations:
        for segment in annotation.segments:
            start_time = convert_offset(segment.segment.start_time_offset)
            end_time = convert_offset(segment.segment.end_time_offset)
            timeline.add_clip('Detected Text',
                              start_time, end_time, annotation.text, segment.confidence)
    save_timeline(asset, timeline)
    return timeline


def save_speech_transcription_timeline(asset, annotations):
    """
    Build a timeline for speech to text transcription.

    Args:
        asset (Asset): The Asset or Asset Id.
        annotations (AnnotateVideoResponse): The Video Intelligence response.

    Returns:
        TimelineBuilder: The populated TimelineBuilder.

    """
    timeline = TimelineBuilder(asset, "gcp-video-speech-transcription")

    for transcription in annotations.speech_transcriptions:
        for alternative in transcription.alternatives:
            if alternative.words:
                # get first and last word
                start_word = alternative.words[0]
                end_word = alternative.words[-1]

                start_time = convert_offset(start_word.start_time)
                end_time = convert_offset(end_word.end_time)
                timeline.add_clip('Speech Transcription',
                                  start_time, end_time, alternative.transcript.strip(),
                                  alternative.confidence)
                break

    save_timeline(asset, timeline)
    return timeline


def save_object_detection_timeline(asset, annotations):
    """
    Build a timeline for video object detection.

    Args:
        asset (Asset): The Asset or Asset Id.
        annotations (AnnotateVideoResponse): The Video Intelligence response.

    Returns:
        Timeline: The populated Timeline.

    """
    timeline = TimelineBuilder(asset, "gcp-video-object-detection")

    for annotation in annotations.object_annotations:
        label = annotation.entity.description
        clip_start = convert_offset(annotation.segment.start_time_offset)
        clip_stop = convert_offset(annotation.segment.end_time_offset)

        timeline.add_clip(label, clip_start, clip_stop,
                          annotation.entity.description, annotation.confidence)

    save_timeline(asset, timeline)
    return timeline


def save_logo_detection_timeline(asset, annotations):
    """
    Build a timeline for video logo detection.

    Args:
        asset (Asset): The Asset or Asset Id.
        annotations (AnnotateVideoResponse): The Video Intelligence response.

    Returns:
        Timeline: The populated Timeline.

    """
    timeline = TimelineBuilder(asset, 'gcp-video-logo-detection')

    for annotation in annotations.logo_recognition_annotations:
        label = annotation.entity.description

        confidence = annotation.tracks[0].confidence
        for segment in annotation.segments:

            clip_start = convert_offset(segment.start_time_offset)
            clip_stop = convert_offset(segment.end_time_offset)

            timeline.add_clip(label, clip_start, clip_stop,
                              annotation.entity.description, confidence)

    save_timeline(asset, timeline)
    return timeline


def save_content_moderation_timeline(asset, annotations):
    """
    Use the explicit annotation to build a timeline.

    Args:
        asset (Asset): The Asset or Asset Id.
        annotations (AnnotateVideoResponse): The Video Intelligence response.

    Returns:
        TimelineBuilder: The timeline.
    """
    legend = [None,
              'Very Unlikely',
              'Unlikely',
              'Possible',
              'Likely',
              'Very Likely']

    timeline = TimelineBuilder(asset, 'gcp-video-explicit-detection')

    current_clip = None
    previous_frame = None

    for frame in annotations.explicit_annotation.frames:
        scrubber = convert_offset(frame.time_offset)

        # Close the current clip of this happens.
        if previous_frame and previous_frame.pornography_likelihood != frame.pornography_likelihood:
            if current_clip:
                current_clip['stop'] = scrubber
                current_clip = None

        if frame.pornography_likelihood > 0:
            if not current_clip:
                current_clip = timeline.add_clip(legend[frame.pornography_likelihood],
                                                 scrubber, scrubber,
                                                 legend[frame.pornography_likelihood])
            else:
                current_clip['stop'] = scrubber

        previous_frame = frame

    save_timeline(asset, timeline)
    return timeline


def save_label_detection_timeline(asset, annotations):
    """
    Use label annotations to build a label detection timeline.

    Args:
        asset (Asset): The Asset or Asset Id.
        annotations (AnnotateVideoResponse): The Video Intelligence response.

    Returns:
        Timeline: The Label Detection timeline.
    """
    timeline = TimelineBuilder(asset, "gcp-video-label-detection")

    def process_label_annotations(results):
        for annotation in results:
            labels = {annotation.entity.description}

            for category in annotation.category_entities:
                labels.add(category.description)

            for seg in annotation.segments:
                clip_start = convert_offset(seg.segment.start_time_offset)
                clip_stop = convert_offset(seg.segment.end_time_offset)

            timeline.add_clip(annotation.entity.description,
                              clip_start, clip_stop, labels, seg.confidence)

    process_label_annotations(annotations.segment_label_annotations)
    process_label_annotations(annotations.shot_label_annotations)
    process_label_annotations(annotations.shot_presence_label_annotations)

    save_timeline(asset, timeline)
    return timeline


def save_speech_to_text_timeline(asset, audio_result):
    """
    Save the results of Speech to Text to a timeline.

    Args:
        asset (Asset): The asset to register the file to.
        audio_result (obj): The speech to text result.

    Returns:
        Timeline: The generated timeline.
    """
    timeline = TimelineBuilder(asset, "gcp-speech-to-text")

    for r in audio_result.results:

        sorted_results = sorted(r.alternatives, key=lambda i: i.confidence, reverse=True)
        best_result = sorted_results[0]

        for result in r.alternatives:
            if result.words:
                # get first and last word
                start_word = result.words[0]
                end_word = result.words[-1]

                start_time = convert_offset(start_word.start_time)
                end_time = convert_offset(end_word.end_time)

                timeline.add_clip(f'Language {r.language_code}', start_time, end_time,
                                  best_result.transcript.strip(), best_result.confidence)

                break

    save_timeline(asset, timeline)
    return timeline


def save_speech_to_text_webvtt(asset, audio_result):
    """
    Create a webvtt file for speech to text.

    Args:
        asset (Asset): The asset to register the file to.
        audio_result (obj): The speech to text result.

    Returns:
        StoredFile
    """
    with WebvttBuilder() as webvtt:
        for r in audio_result.results:

            sorted_results = sorted(r.alternatives, key=lambda i: i.confidence, reverse=True)
            best_result = sorted_results[0]

            for result in r.alternatives:
                if result.words:
                    # get first and last word
                    start_word = result.words[0]
                    end_word = result.words[-1]

                    start_time = convert_offset(start_word.start_time)
                    end_time = convert_offset(end_word.end_time)
                    webvtt.append(start_time, end_time, best_result.transcript.strip())
                    break

    logger.info(f'Saving speech-to-text data from {webvtt.path}')
    sf = file_storage.assets.store_file(webvtt.path, asset,
                                        'captions',
                                        'gcp-speech-to-text.vtt')
    return webvtt.path, sf


def save_video_speech_transcription_webvtt(asset, annotations):
    """
    Build a timeline for video speech transcription.

    Args:
        asset (Asset): The Asset or Asset Id.
        annotations (AnnotateVideoResponse): The Video Intelligence response.

    Returns:
        tuple: The path to the vtt file and StoredFile object

    """
    with WebvttBuilder() as webvtt:
        for transcription in annotations.speech_transcriptions:
            for alternative in transcription.alternatives:
                if alternative.words:
                    # get first and last word
                    start_word = alternative.words[0]
                    end_word = alternative.words[-1]

                    start_time = convert_offset(start_word.start_time)
                    end_time = convert_offset(end_word.end_time)
                    webvtt.append(start_time, end_time, alternative.transcript.strip())
                    break

    logger.info(f'Saving video_speech_transcription webvtt from {webvtt.path}')
    sf = file_storage.assets.store_file(webvtt.path, asset,
                                        'captions',
                                        'gcp-video-speech-transcription.vtt')
    return webvtt.path, sf


def convert_offset(offset):
    """
    Convert the GCP seconds/nanos to a float.

    Args:
        offset (object): A GCP time object.

    Returns:
        float: A float representing seconds and nanos.

    """
    if not offset:
        return 0
    else:
        return offset.seconds + (offset.nanos / 1000000000.0)
