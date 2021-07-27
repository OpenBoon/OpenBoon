from types import SimpleNamespace

import pytest
from google.cloud.pubsub_v1.subscriber.message import Message

from metrics.management.commands.pubsublistener import callback
from metrics.records.models import ApiCall

pytestmark = pytest.mark.django_db


def test_pubsub_message_callback():
    payload = '[{"asset_id":"pBbuB7fgST2faIKXAF1ZU4gZEDWzT03c","asset_path":"gs://zorroa-public/demo-files/death-valley-trail.mp4","asset_type":"video","services":["standard","aws-label-detection","gcp-video-object-detection"],"length":6.571}]'
    pubsub_message = SimpleNamespace()
    pubsub_message.attributes = {'project_id': '1a0cbcd6-cf49-4992-a858-7966400082da',
                                 'type': 'assets-indexed'}
    pubsub_message.data = payload.encode('utf-8')
    pubsub_message.message_id = '2765943120819895'
    pubsub_message.publish_time = SimpleNamespace(seconds=1627084759, nanos=306615)
    pubsub_message.ordering_key = 5
    pubsub_message.ByteSize = lambda *args: len(pubsub_message.data)
    message = Message(pubsub_message, '1', 1, None)
    callback(message)
    api_call = ApiCall.objects.first()
    assert api_call.video_seconds == 6.571
    assert str(api_call.project) == '1a0cbcd6-cf49-4992-a858-7966400082da'
