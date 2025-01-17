#!/usr/bin/env python3
import datetime
import logging
import multiprocessing
import os
import time
import queue
import urllib3

import backoff
import jwt
import requests
from flask import Flask
from gevent.pywsgi import WSGIServer
from google.cloud import pubsub_v1

logger = logging.getLogger('swivel')
logging.basicConfig(level=logging.INFO)

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

app = Flask(__name__)


def message_poller(xqueue, project, sub):
    """
    Polls for messages from PubSub.  Takes a managed multiprocessing queue to store
    the message in before calling the webhook described by the message.  Once
    the message is in the queue it gets acked.

    Args:
        xqueue (Queue): A multiprocessing queue.
        project (str): Gcloud project
        sub (str): Pubsub/subscription.
    """
    def callback(msg):
        try:
            # You can't put the pubsub message in a multiprocess queue directly
            # because it's not serializable.
            xqueue.put({
                'url': msg.attributes['url'],
                'trigger': msg.attributes['trigger'],
                'project_id': msg.attributes['project_id'],
                'secret_key': msg.attributes['secret_key'],
                'data': msg.data
            }, block=True, timeout=30)
            msg.ack()
        except queue.Full:
            # If the queue gets full then an exception is thrown
            # and we don't ack the message.
            logger.warning('Internal Queue is full')

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


def webhook_worker(xqueue):
    """
    This function polls a managed Queue for PubSub messages and calls
    the webhook.

    Args:
        xqueue (Queue): A multiprocessing Queue.
    """
    while True:
        # Blocks until something appears.
        item = xqueue.get()
        try:
            call_webhook(item)
        except Exception as e:
            logger.error('Unable to execute web hook.', e)


@backoff.on_exception(backoff.expo,
                      (requests.exceptions.Timeout, requests.exceptions.ConnectionError),
                      max_tries=2)
def call_webhook(msg):
    """
    Makes the actual call to the webhook URL.  Tries twice and then
    gives up.

    Args:
        msg (dict): The PubSub message.
    """
    url = msg['url']

    headers = {
        'X-BoonAI-Signature-256': generate_token(msg),
        'X-BoonAI-Trigger': msg['trigger'],
        'X-BoonAI-ProjectId': msg['project_id'],
        'Content-Type': 'application/json'
    }
    rsp = requests.post(url, data=msg['data'], headers=headers, verify=False, timeout=3)
    logger.info(f'web hook status=[{rsp.status_code}] url={url}')
    return rsp


def generate_token(msg):
    """
    Generates a JWT token for the request.

    Args:
        msg (dict): The PubSub message.

    Returns:
        str:  A JWT token.
    """
    claims = {
        'exp': datetime.datetime.utcnow() + datetime.timedelta(seconds=60)
    }

    return jwt.encode(claims, msg['secret_key'], algorithm='HS256')


def create_localdev_env(project, sub):
    """
    Create a local dev environment if the PUBSUB_EMULATOR_HOST env
    variable is set.
    """
    host = os.environ.get('PUBSUB_EMULATOR_HOST')
    logger.info(f'Local Development mode enabled at {host}')

    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(project, 'webhooks')
    publisher.create_topic(request={'name': topic_path})

    subscriber = pubsub_v1.SubscriberClient()
    subscription_path = subscriber.subscription_path(project, sub)

    with subscriber:
        subscriber.create_subscription(
            request={'name': subscription_path, 'topic': topic_path}
        )

    logger.info(f'Creating test environment topic: {topic_path}')
    logger.info(f'Creating test environment sub: {subscription_path}')


@app.route('/health')
def health():
    return 'OK', 200


@app.route('/queue')
def get_queue_size():
    """
    Returns the size of the queue.

    Returns:
        tuple (str, int): The size of the queue.
    """
    return str(GlobalQueue.mqueue.qsize()), 200


class GlobalQueue:
    """
    Stores a global copy of the managed queue.
    """
    # Setup a multiprocessing producer/consumer queue
    queue_size = int(os.environ.get('SWIVEL_QUEUE_SIZE', '10000'))
    manager = multiprocessing.Manager()
    mqueue = manager.Queue(queue_size)


if __name__ == '__main__':

    project_name = os.environ['GCLOUD_PROJECT']
    sub_name = os.environ['SWIVEL_SUBSCRIPTION']

    if 'PUBSUB_EMULATOR_HOST' in os.environ:
        create_localdev_env(project_name, sub_name)

    poller = multiprocessing.Process(multiprocessing.Process(
        target=message_poller, args=(GlobalQueue.mqueue, project_name, sub_name)).start())

    threads = int(os.environ.get('SWIVEL_THREADS', '10'))
    pool = multiprocessing.Pool(threads, webhook_worker, (GlobalQueue.mqueue,))

    # Disables all these flask endpoint logs because the health check
    # fills the logs with garbage.
    flask_log = logging.getLogger('werkzeug')
    flask_log.disabled = True
    app.logger.disabled = True

    port = int(os.environ.get("SWIVEL_PORT", "5000"))
    logger.info(f'Swivel Listening on port: {port}')
    logger.info(f'GCP Project: {project_name}')
    logger.info(f'Subscription: {sub_name}')

    server = WSGIServer(('0.0.0.0', port), app, log=None)
    server.serve_forever()
