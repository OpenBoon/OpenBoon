#!/usr/bin/env python3
import logging
import multiprocessing
import os
import shutil
import subprocess
import tempfile
import time

import urllib3
import yaml
from flask import Flask
from gevent.pywsgi import WSGIServer
from google.cloud import pubsub_v1

logger = logging.getLogger('tugboat')
logging.basicConfig(level=logging.INFO)

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

app = Flask(__name__)


@app.route('/health')
def health():
    return 'OK', 200


def message_poller(sub):
    """
    Polls for messages from PubSub and kicks off a model deployment job.

    Args:
        sub (str): PubSub/subscription.
    """
    def callback(msg):
        try:
            if msg.attributes['type'] == "model-upload":
                spec = dict(msg.attributes)
                build_and_deploy(spec)
        except Exception as e:
            logger.error("Error handling pubsub message, ", e)
        finally:
            msg.ack()

    project_name = os.environ['GCLOUD_PROJECT']
    subscriber = pubsub_v1.SubscriberClient()
    subscription_path = subscriber.subscription_path(project_name, sub)
    streaming_pull_future = subscriber.subscribe(subscription_path, callback=callback)

    logger.info(f'Listening to: {sub}')
    while True:
        with subscriber:
            try:
                streaming_pull_future.result()
            except Exception as e:
                logger.warning('Unexpected error polling pus/sub, reconnecting...', e)
                time.sleep(5)


def build_and_deploy(spec):
    """
    Build and deploy a container which has the uploaded model file.

    Args:
        spec (dict): Tne spec for the container.

    Returns:
        boolean: True on success
    """
    logger.info(f'Building {spec}')
    model_type = spec['modelType']

    tmlp_path = os.environ.get('TEMPLATE_PATH', '/app/tmpl')
    if model_type.startswith('TORCH_'):
        tmpl = f'{tmlp_path}/torch'
    elif model_type == 'TF_SAVED_MODEL':
        tmpl = f'{tmlp_path}/tf'
    else:
        logger.error(f'The model type {model_type} has no template')
        return False

    # Copy the model and the template into a temp dir.
    # Then submit the temp dir to be built.
    d = tempfile.mkdtemp()
    build_dir = os.path.join(d, 'build')
    logger.info(f'copying {tmpl} template to dir {build_dir}')
    try:
        shutil.copytree(tmpl, build_dir)
        submit_build(spec, build_dir)
    finally:
        shutil.rmtree(d)
    return True


def submit_build(spec, build_path):
    """
    Submit a build to google cloud build. The submission is async how the function may
    need time to package up the files.

    Args:
        spec (dict): The spec for the build.
        build_path (str): The path to the files that make up the build.

    """
    build_file = generate_build_file(spec, build_path)
    run_cloud_build(build_file, build_path)


def generate_build_file(spec, build_path):
    """
    Generates a Cloud Build Yaml file.

    Args:
        spec (dict): The spec from Pub/Sub
        build_path (str): The path to the docker build directory.

    Returns:
        str: The path to the build file.

    """
    img = spec['image']
    model_id = spec['modelId']
    model_file = spec['modelFile']

    build = {
        'steps': [
            {
                'name': 'gcr.io/cloud-builders/docker',
                'args': ['build', '-t', img, '--build-arg', f'MODEL_URL={model_file}', '.']
            },
            {
                'name': 'gcr.io/cloud-builders/docker',
                'args':  ['push', img]
            },
            {
                'name': 'gcr.io/google.com/cloudsdktool/cloud-sdk',
                'entrypoint': 'gcloud',
                'args': ['run', 'deploy', f'mod-{model_id}', '--image', img,
                         '--region', 'us-central1',
                         '--platform', 'managed',
                         '--ingress', 'internal',
                         '--clear-vpc-connector',
                         '--allow-unauthenticated',
                         '--memory=2Gi',
                         '--max-instances', '4',
                         '--labels', f'model-id={model_id}']
            }
        ],
        'images': [
            img
        ],
        'name': f'model-{model_id}-{int(time.time())}'
    }

    build_file = f'{build_path}/cloudbuild.yaml'
    with open(build_file, 'w') as fp:
        yaml.dump(build, fp)
    return build_file


def run_cloud_build(build_file, build_path):
    cmd = [
        'gcloud',
        'builds',
        'submit',
        '--async',
        '--config',
        build_file,
        build_path
    ]
    logger.info('Running: {}'.format(cmd))
    subprocess.check_call(cmd)


def create_localdev_env(project, sub):
    """
    Create a local dev environment if the PUBSUB_EMULATOR_HOST env
    variable is set.
    """
    host = os.environ.get('PUBSUB_EMULATOR_HOST')
    logger.info(f'Local Development mode enabled at {host}')

    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(project, 'model-events')
    logger.info("creating topic " + topic_path)
    publisher.create_topic(request={'name': topic_path})

    subscriber = pubsub_v1.SubscriberClient()
    subscription_path = subscriber.subscription_path(project, sub)

    with subscriber:
        subscriber.create_subscription(
            request={'name': subscription_path, 'topic': topic_path}
        )

    logger.info(f'Creating test environment topic: {topic_path}')
    logger.info(f'Creating test environment sub: {subscription_path}')


if __name__ == "__main__":

    sub_name = "tugboat-model-events"
    project_name = os.environ['GCLOUD_PROJECT']

    if 'PUBSUB_EMULATOR_HOST' in os.environ:
        create_localdev_env(project_name, sub_name)

    poller = multiprocessing.Process(multiprocessing.Process(
        target=message_poller, args=(sub_name,)).start())

    # Disables all these flask endpoint logs because the health check
    # fills the logs with garbage.
    flask_log = logging.getLogger('werkzeug')
    flask_log.disabled = True
    app.logger.disabled = True

    port = int(os.environ.get('PORT', 9393))
    logger.info(f'Starting Tugboat on port: {port}')
    server = WSGIServer(('0.0.0.0', port), app, log=None)
    server.serve_forever()