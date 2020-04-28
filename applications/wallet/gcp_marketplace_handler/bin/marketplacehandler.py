#!/usr/bin/env python3

import json
import os
import pprint
import subprocess
from time import sleep

from google.cloud import pubsub_v1
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

PROJECT_ID = os.environ['GOOGLE_CLOUD_PROJECT']
PUBSUB_SUBSCRIPTION = os.environ['MARKETPLACE_SUBSCRIPTION']
PROCUREMENT_API = 'cloudcommerceprocurement'
DJANGO_SETTINGS_FILE = os.environ.get('MARKETPLACE_DJANGO_SETTINGS',
                                      'wallet.settings')


class MessageHandler(object):

    def __init__(self, message):
        self.message = message
        self.payload = json.loads(message.data)
        self.event_type = self.payload['eventType']
        self.service = build(PROCUREMENT_API, 'v1', cache_discovery=False)

    def handle(self):
        print('Received message:')
        pprint.pprint(self.payload)

        # Step 1. User has requested to purchase a subscription.
        if self.event_type == 'ENTITLEMENT_CREATION_REQUESTED':
            self._handle_entitlement_creation_requested()

        elif self.event_type == 'ACCOUNT_ACTIVE':
            self._handle_account_activate()

        elif self.event_type == 'ENTITLEMENT_ACTIVE':
            self._handle_entitlement_active()

        elif self.event_type == 'ENTITLEMENT_CANCELLED':
            self._handle_entitlement_cancelled()

        self.message.ack()

    def _handle_entitlement_cancelled(self):
        entitlement_id = self.payload['entitlement']['id']
        self._django_command('deactivateproject', entitlement_id)

    def _handle_entitlement_active(self):
        entitlement_id = self.payload['entitlement']['id']
        entitlement = self._get_entitlement(entitlement_id)
        self._django_command('createproject', entitlement_id, f'marketplace-{entitlement_id}',
                             '100', '10000', entitlement['account'])

    def _handle_entitlement_creation_requested(self):
        entitlement_id = self.payload['entitlement']['id']
        entitlement = self._get_entitlement(entitlement_id)
        self._approve_account(entitlement['account'])  # TODO: remove
        if not entitlement:
            print('No entitlement found. Could handle entitlement creation request.')
            return
        if entitlement['state'] == 'ENTITLEMENT_ACTIVATION_REQUESTED':
            name = self._get_entitlement_name(entitlement_id)
            try:
                request = self.service.providers().entitlements().approve(
                    name=name, body={})
                request.execute()
            except HttpError as err:
                if err.resp.status == 400:
                    self._approve_account(entitlement['account'])
                    request.execute()


    def _get_entitlement(self, entitlement_id):
        """Gets an entitlement from the Procurement Service."""
        name = self._get_entitlement_name(entitlement_id)
        request = self.service.providers().entitlements().get(name=name)
        try:
            response = request.execute()
            return response
        except HttpError as err:
            if err.resp.status == 404:
                return None

    def _get_entitlement_name(self, entitlement_id):
        return 'providers/DEMO-{}/entitlements/{}'.format(PROJECT_ID, entitlement_id)

    def _approve_account(self, name):
        self._django_command('createuser', name)
        request = self.service.providers().accounts().approve(
            name=name, body={'approvalName': 'signup'})
        request.execute()

    def _handle_account_activate(self):
        name = self._get_account_name(self.payload['account']['id'])
        self._django_command('createuser', name)

    def _get_account_name(self, account_id):
        return 'providers/DEMO-{}/accounts/{}'.format(PROJECT_ID,
                                                      account_id)

    def _get_account(self, name):
        request = self.service.providers().accounts().get(name=name)
        try:
            response = request.execute()
            return response
        except HttpError as err:
            if err.resp.status == 404:
                return None

    def _django_command(self, command, *args):
        subprocess.check_call(['python', '../../app/manage.py', command, f'--settings={DJANGO_SETTINGS_FILE}',
                               *args])


def main():
    """Main entrypoint to the integration with the Procurement Service."""

    def callback(message):
        """Callback for handling Cloud Pub/Sub messages."""
        MessageHandler(message).handle()

    subscriber = pubsub_v1.SubscriberClient()
    subscription_path = subscriber.subscription_path(PROJECT_ID,
                                                     PUBSUB_SUBSCRIPTION)
    subscription = subscriber.subscribe(subscription_path, callback=callback)
    print('Listening for messages on {}'.format(subscription_path))
    print('Exit with Ctrl-\\')
    while True:
        try:
            subscription.result()
            sleep(5)
        except Exception as exception:
            print('Listening for messages on {} threw an Exception: {}.'.format(
                subscription_path, exception))


if __name__ == '__main__':
    main()
