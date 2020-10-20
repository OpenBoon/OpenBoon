import json
import os
from unittest.mock import patch

import zmlp_analysis.aws.timeline as timeline
from zmlpsdk import file_storage
from zmlpsdk.testing import TestAsset
from .test_transcribe import load_results


@patch.object(file_storage.assets, 'store_blob')
def test_save_raw_transcribe_result(store_patch):
    asset = TestAsset()
    result = load_results('transcribe.pk')
    timeline.save_raw_transcribe_result(asset, result)

    blob = store_patch.call_args_list[0][0][0]
    data = json.loads(blob)

    assert data['jobName'] == '5e2ce15a-77ac-4637-95dc-e31f96b9c180'
    assert data['results']['transcripts'][0]['transcript'] == \
           'sanitation. There\'s more coming sanitation, toilet on poop and I have yet to emerge.'


def test_generate_transcribe_sentences():
    result = load_results('transcribe.pk')
    items = list(timeline.generate_transcribe_sentences(result))

    assert 2 == len(items)
    assert 'sanitation.' == items[0]['sentence']
    assert 'There\'s more coming sanitation, toilet ' \
           'on poop and I have yet to emerge.' == items[1]['sentence']

    assert 1.0 == items[0]['confidence']
    assert 0.6969 == items[1]['confidence']

    assert 0.09 == items[0]['start_time']
    assert 1.04 == items[0]['end_time']


@patch.object(file_storage.assets, 'store_file')
def test_save_transcribe_webvtt(store_patch):
    store_patch.return_value = {}

    asset = TestAsset()
    result = load_results('transcribe.pk')
    path, sf = timeline.save_transcribe_webvtt(asset, result)

    with open(path, 'r') as fp:
        vttgen = fp.read()

    with open(os.path.dirname(__file__) + '/mock-data/transcribe.vtt', 'r') as fp:
        vttgood = fp.read()

    assert vttgen == vttgood


@patch('zmlp_analysis.aws.timeline.save_timeline')
def test_save_transcribe_timeline(save_patch):
    save_patch.return_value = None

    asset = TestAsset()
    result = load_results('transcribe.pk')

    tlb = timeline.save_transcribe_timeline(asset, result)
    track = tlb.tracks['Language en-US']
    assert track['name'] == 'Language en-US'
    assert len(track['clips']) == 2
    assert 'sanitation.' == track['clips'][0]['content'][0]
