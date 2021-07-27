import json

from django.conf import settings
from django.core.management import BaseCommand
from google.cloud import pubsub_v1
from google.oauth2 import service_account

from metrics.records.models import ApiCall


def callback(message):
    """Callback for handling Cloud Pub/Sub messages."""
    if message.attributes['type'] != 'assets-indexed':
        message.ack()
        return
    metrics = json.loads(message.data)
    project_id = message.attributes['project_id']
    records = []
    for metric in metrics:
        for service in metric['services']:
            length = metric['length']
            asset_type = metric['asset_type']
            record = ApiCall(project=project_id,
                             service=service,
                             asset_id=metric['asset_id'],
                             asset_path=metric['asset_path'])
            if asset_type == 'video':
                record.video_seconds = length
            elif asset_type in ['image', 'document']:
                record.image_count == length
            else:
                raise TypeError(f'{asset_type} is not a supported metric type.')
            records.append(record)
    created_records = ApiCall.objects.bulk_create(records)
    for record in created_records:
        log = dict(severity='INFO',
                   log_type='record-created',
                   project_id=record.project,
                   service=record.service,
                   asset_id=record.asset_id,
                   asset_path=record.asset_path,
                   video_seconds=record.video_seconds,
                   image_count=record.image_count)
        print(json.dumps(log))


class Command(BaseCommand):
    help = ('Starts Pub/Sub listener subscribed to the archivist metrics topic. Messages are read'
            'and converted to entries in the metrics database.')

    def handle(self, *args, **options):
        credentials = service_account.Credentials.from_service_account_info(
            json.loads(settings.PUB_SUB_CREDENTIALS))
        subscriber = pubsub_v1.SubscriberClient(credentials=credentials)
        subscription_path = subscriber.subscription_path(settings.PROJECT_ID,
                                                         settings.PUBSUB_SUBSCRIPTION)
        subscription = subscriber.subscribe(subscription_path, callback=callback)
        print('Listening for messages on {}'.format(subscription_path))
        print('Exit with Ctrl-\\')
        try:
            subscription.result()
        except KeyboardInterrupt:
            print('Program terminated by user. Goodbye.')
            return
