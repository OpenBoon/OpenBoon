#!/usr/bin/env python3
import logging
import os
import shutil
import tempfile
import time
import queue
import urllib3
import json
import subprocess
import multiprocessing

from google.cloud import pubsub_v1
from flask import Flask

logger = logging.getLogger('swivel')
logging.basicConfig(level=logging.INFO)

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

app = Flask(__name__)


@app.route("/")
def hello_world():
    name = os.environ.get("NAME", "World")
    return "Hello {}!".format(name)


def message_poller(project, sub):
    """
    Polls for messages from PubSub.  Takes a managed multiprocessing queue to store
    the message in before calling the webhook described by the message.  Once
    the message is in the queue it gets acked.

    Args:
        project (str): Gcloud project
        sub (str): Pubsub/subscription.
    """
    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(project, 'model-events')

    def callback(msg):
        if msg.attributes['type'] == "model-upload":
            spec = {
                msg.attributes['image'],
                msg.attributes['service']
            }
            build = build_and_deploy(spec)
            msg = {
                "type": "model-deploy",
                "modelId":  msg.attributes['modelId'],
                "buildId": build['buildId']
            }
            publisher.publish(topic_path, json.dumps(msg).encode())

        msg.ack()

    subscriber = pubsub_v1.SubscriberClient()
    subscription_path = subscriber.subscription_path(project, sub)
    streaming_pull_future = subscriber.subscribe(subscription_path, callback=callback)


    while True:
        with subscriber:
            try:
                streaming_pull_future.result()
            except Exception as e:
                logger.warning('Unexpected error polling pus/sub, reconnecting', e)
                time.sleep(5)

def build_and_deploy(spec):
    """
    Build and deploy the container.

    Args:
        spec (dict): Tne spec for the container.


    """
    d = tempfile.mkdtemp()
    try:
        shutil.copytree("/app/tmpl/torch", d, dirs_exist_ok=True)
        download_model(spec['modelFile'], d)
        copy_template(spec, d)
        subprocess.check_call(['gcloud', 'builds', 'submit', d])
    finally:
        shutil.rmtree(d)


def copy_template(spec, build_dir):
    """
    Copy the Cloud Build template and replace some values.

    Args:
        spec:
        build_dir:

    Returns:

    """
    with open('cloudbuild.yaml', 'r') as fp:
        data = fp.read()

    data = data.replace('PROJECT_ID', os.environ['GCLOUD_PROJECT'])
    data = data.replace('IMAGE', spec['image'])
    data = data.replace('SERVICE_NAME', spec['service'])
    data = data.replace('REGION', 'us-central1')

    with open(os.path.join(build_dir, 'cloudbuild.yaml', 'w')) as fp:
        fp.write(data)


def download_model(src_uri, dst):
    subprocess.check_call(['gsutil', 'cp', src_uri, dst])


def create_localdev_env(project, sub):
    """
    Create a local dev environment if the PUBSUB_EMULATOR_HOST env
    variable is set.
    """
    host = os.environ.get('PUBSUB_EMULATOR_HOST')
    logger.info(f'Local Development mode enabled at {host}')

    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(project, 'model-events')
    publisher.create_topic(request={'name': topic_path})

    subscriber = pubsub_v1.SubscriberClient()
    subscription_path = subscriber.subscription_path(project, sub)

    with subscriber:
        subscriber.create_subscription(
            request={'name': subscription_path, 'topic': topic_path}
        )

    logger.info(f'Creating test environment topic: {topic_path}')
    logger.info(f'Creating test environment sub: {subscription_path}')


# Message a new model was uploaded comes in.
# {
#    "modelId": "blah",
#    "projectId": "blah",
#    "modelFile": "blah",
#    "modelType": "blah"
#    "moduleName" "blah",
#    "serviceNane", "blah blah",
#    "memory": "2g",
#    "cpu": 1
# }

#  gcloud run deploy --image gcr.io/zvi-dev/test --platform managed --ingress='internal' --clear-vpc-connector
#  gcloud run services describe test --platform managed --region  us-central1 --format 'value(status.url)'










if __name__ == "__main__":

    project_name = os.environ['GCLOUD_PROJECT']
    sub_name = os.environ['MODEL_EVENT_SUBSCRIPTION']

    poller = multiprocessing.Process(multiprocessing.Process(
        target=message_poller, args=(project_name, sub_name)).start())

    # Disables all these flask endpoint logs because the health check
    # fills the logs with garbage.
    flask_log = logging.getLogger('werkzeug')
    flask_log.disabled = True
    app.logger.disabled = True

    app.run(debug=True, host="0.0.0.0", port=int(os.environ.get("PORT", 9393)))
