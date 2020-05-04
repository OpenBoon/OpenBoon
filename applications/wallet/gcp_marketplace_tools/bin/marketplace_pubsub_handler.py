#!/usr/bin/env python3

import json
import os
import pathlib
import pprint
import subprocess
from time import sleep

import sentry_sdk
from google.cloud import pubsub_v1
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

sentry_sdk.init(dsn='https://5c1ab0d8be954c35b92283c1290e9924@o280392.ingest.sentry.io/5218609')

PROJECT_ID = os.environ['GOOGLE_CLOUD_PROJECT']
PUBSUB_SUBSCRIPTION = os.environ['MARKETPLACE_SUBSCRIPTION']
PROCUREMENT_API = 'cloudcommerceprocurement'
DJANGO_SETTINGS_MODULE = os.environ.get('DJANGO_SETTINGS_MODULE',
                                      'wallet.settings')
PYTHON = os.environ.get('PYTHON', 'python3')


class MessageHandler(object):
    """Handles pub sub messages from the gcp marketplace.

    Args:
        message(pubsub_v1.subscriber.message.Message): Message to handle.
        credentials(service_account.Credentials): GCP credentials object to use for auth.

    """

    def __init__(self, message, credentials):
        self.message = message
        self.payload = json.loads(message.data)
        self.event_type = self.payload['eventType']
        self.service = build(PROCUREMENT_API, 'v1', cache_discovery=False, credentials=credentials)

    def handle(self):
        """Handle a pub/sub message send by the gcp marketplace."""

        # For information on what these events mean view the gcp marketplace docs.
        # https://cloud.google.com/marketplace/docs/partners/integrated-saas/backend-integration#eventtypes  # noqa
        event_map = {'ENTITLEMENT_CREATION_REQUESTED': self._handle_entitlement_creation_requested,
                     'ACCOUNT_ACTIVE': self._handle_account_activate,
                     'ENTITLEMENT_ACTIVE': self._handle_entitlement_active,
                     'ENTITLEMENT_CANCELLED': self._handle_entitlement_cancelled}

        if self.event_type in event_map:
            event_map[self.event_type]()
        else:
            print(f'Ignoring unknown event type {self.event_type}: {self.payload}.')
        self.message.ack()

    def _handle_entitlement_creation_requested(self):
        """Handles the ENTITLEMENT_CREATION_REQUESTED event from google marketplace."""
        self._print_handling_message()
        entitlement_id = self.payload['entitlement']['id']
        entitlement = self._get_entitlement(entitlement_id)
        self._approve_account(entitlement['account'])  # TODO: remove
        if not entitlement:
            print('No entitlement found. Could not handle entitlement creation request.')
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
            print(f'Approved entitlement {entitlement_id}.')

    def _handle_account_activate(self):
        """Handles the ACCOUNT_ACTIVE event from google marketplace."""
        self._print_handling_message()
        name = self._get_account_name(self.payload['account']['id'])
        self._django_command('createuser', name)
        print(f'User created for account: {name}')

    def _handle_entitlement_active(self):
        """Handles the ENTITLEMENT_ACTIVE event from google marketplace."""
        self._print_handling_message()
        entitlement_id = self.payload['entitlement']['id']
        entitlement = self._get_entitlement(entitlement_id)
        project_name = f'marketplace-{entitlement_id}'
        self._django_command('createproject', entitlement_id, project_name,
                             '100', '10000', entitlement['account'])
        print(f'Project {entitlement_id} created.')

    def _handle_entitlement_cancelled(self):
        """Handles the ENTITLEMENT_CANCELLED event from google marketplace."""
        self._print_handling_message()
        entitlement_id = self.payload['entitlement']['id']
        self._django_command('deactivateproject', entitlement_id)
        print(f'Deactivated project {entitlement_id}.')

    def _print_handling_message(self):
        """Prints out information about the message being handled."""
        print(f'Handling {self.event_type} Event: {self.payload}')

    def _get_entitlement(self, entitlement_id):
        """Gets an entitlement from the Procurement Service.

        Args:
            entitlement_id(str): UUID of the entitlement to retrieve.

        Returns(dict): Dictionary representing an entitlement.

        """
        name = self._get_entitlement_name(entitlement_id)
        request = self.service.providers().entitlements().get(name=name)
        try:
            response = request.execute()
            return response
        except HttpError as err:
            if err.resp.status == 404:
                return None

    def _get_entitlement_name(self, entitlement_id):
        """Returns the full name of an entitlement.

        Args:
            entitlement_id(str): UUID of the entitlement to retrieve.

        Returns(dict): Name of the entitlement.

        """
        return 'providers/DEMO-{}/entitlements/{}'.format(PROJECT_ID, entitlement_id)

    def _approve_account(self, account_name):
        """Creates an approval on a marketplace account. And creates the a corresponding
        account in the Wallet db.

        TODO: Need to move the account creation to a signup flow.

        Args:
            account_name(str): Full account name of the marketplace account.

        """
        self._django_command('createuser', account_name)
        request = self.service.providers().accounts().approve(
            name=account_name, body={'approvalName': 'signup'})
        request.execute()

    def _get_account_name(self, account_id):
        """Returns the full name of a marketplace account.

        Args:
            account_id(str): UUID of the account to retrieve.

        Returns(dict): Name of the account.

        """
        return 'providers/DEMO-{}/accounts/{}'.format(PROJECT_ID, account_id)

    def _django_command(self, command, *args):
        """Shells out to a django management command.

        Args:
            command(str): Django command to run.
            args(str): Any additional arguments to pass to the Django command.
        """
        manage_path = pathlib.Path(__file__).parent.joinpath('../../app/manage.py').absolute()
        command = [PYTHON, manage_path, command,
                   f'--settings={DJANGO_SETTINGS_MODULE}', *args]
        try:
            subprocess.run(command, capture_output=True, check=True)
        except Exception as exception:
            pprint.pprint(exception.stderr)
            raise exception


def main():
    """Main entrypoint runs a google marketplace watcher. This script listens to and
    handles google marketplace pub/sub events."""
    credentials = service_account.Credentials.from_service_account_info(
        json.loads(os.environ['GOOGLE_CREDENTIALS']))

    def callback(message):
        """Callback for handling Cloud Pub/Sub messages."""
        MessageHandler(message, credentials).handle()

    subscriber = pubsub_v1.SubscriberClient(credentials=credentials)
    subscription_path = subscriber.subscription_path(PROJECT_ID,
                                                     PUBSUB_SUBSCRIPTION)
    subscription = subscriber.subscribe(subscription_path, callback=callback)
    print('Listening for messages on {}'.format(subscription_path))
    print('Exit with Ctrl-\\')
    while True:
        subscription.result()
        sleep(5)


if __name__ == '__main__':
    main()
