#!/usr/bin/env python3
import datetime
import json
import os
import pathlib
import pprint
import subprocess
import sys
import uuid
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
SERVICE_NAME = os.environ.get('MARKETPLACE_SERVICE_NAME', 'isaas-codelab.mp-marketplace-partner-demos.appspot.com')  #TODO: Remove demo service.
DJANGO_SETTINGS_MODULE = os.environ.get('DJANGO_SETTINGS_MODULE', 'wallet.settings')
PYTHON = os.environ.get('PYTHON', 'python3')


def django_command(self, command, *args):
    """Shells out to a django management command.

    Args:
        command(str): Django command to run.
        args(str): Any additional arguments to pass to the Django command.
    """
    manage_path = pathlib.Path(__file__).parent.joinpath('../../app/manage.py').absolute()
    command = [PYTHON, manage_path, command,
               f'--settings={DJANGO_SETTINGS_MODULE}', *args]
    try:
        process = subprocess.run(command, capture_output=True, check=True)
        return process.stdout
    except Exception as exception:
        pprint.pprint(exception.stderr)
        raise exception
    
    
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
        django_command('createuser', name)
        print(f'User created for account: {name}')

    def _handle_entitlement_active(self):
        """Handles the ENTITLEMENT_ACTIVE event from google marketplace."""
        self._print_handling_message()
        entitlement_id = self.payload['entitlement']['id']
        entitlement = self._get_entitlement(entitlement_id)
        project_name = f'marketplace-{entitlement_id}'
        django_command('createproject', entitlement_id, project_name, '100', '10000',
                       entitlement['account'])  # Use the correct plan limits.
        print(f'Project {entitlement_id} created.')

    def _handle_entitlement_cancelled(self):
        """Handles the ENTITLEMENT_CANCELLED event from google marketplace."""
        self._print_handling_message()
        entitlement_id = self.payload['entitlement']['id']
        django_command('deactivateproject', entitlement_id)
        print(f'Deactivated project {entitlement_id}.')

    def _print_handling_message(self):
        """Prints out information about the message being handled."""
        print(f'Handling {self.event_type} \nEvent: {pprint.pformat(self.payload)}')

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
        django_command('createuser', account_name)
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


class UsageReporter():
    # Handles reporting project usage to Googel Marketplace.
    def __init__(self, credentials):
        self.procurement_api = build(PROCUREMENT_API, 'v1', cache_discovery=False, credentials=credentials)
        self.service_api = build('servicecontrol', 'v1', credentials=credentials)

    def report(self):
        """Loops over all active entitlements and sends usage information for each of them."""
        entitlements = self._get_active_entitlements()
        for entitlement in entitlements:
            self._report_usage(entitlement)

    def _get_active_entitlements(self):
        """Returns a list of all active marketplace entitlements."""
        request = self.procurement_api.providers().entitlements().list(
            parent=f'providers/DEMO-{PROJECT_ID}',
            filter='state=active')
        return request.execute()['entitlements']

    def _get_usage(self, entitlement):
        """Returns usage info for the project linked to the entitlement given."""

        # TODO: Removed once the Zmlp API is updated.
        return {'end_time': 1588701600,
                'video_hours': 1,
                'image_count': 2}

        project_id = entitlement['name'].split('/')[-1]
        usage = django_command('gethourlyusage', project_id)
        return json.loads(usage)

    def _report_usage(self, entitlement):
        """Sends usage information to marketplace for the given entitlement."""
        time_format = '%Y-%m-%dT%H:%M:%SZ'
        usage = self._get_usage(entitlement)
        end_time = datetime.datetime.fromtimestamp(usage['end_time'], datetime.timezone.utc)
        start_time = end_time - datetime.timedelta(hours=1)
        operation = {
            'operationId': str(uuid.uuid4()),
            'operationName': 'Codelab Usage Report',
            'consumerId': entitlement['usageReportingId'],
            'startTime': start_time.strftime(time_format),
            'endTime': end_time.strftime(time_format),
            'metricValueSets': [{
                'metricName': f'{SERVICE_NAME}/{entitlement["plan"]}_requests',  #TODO: Get real service name.
                'metricValues': [{
                    'int64Value': usage['image_count'],  # TODO: Get from usage.
                }],
            }],
        }
        check = self.service_api.services().check(
            serviceName=SERVICE_NAME, body={
                'operation': operation
            }).execute()

        if 'checkErrors' in check:
            print('Errors for user %s with product %s:' % (entitlement['account'],
                                                           entitlement['product']))
            print(check['checkErrors'])
            ### TODO: Temporarily turn off service for the user. ###
            return
        print(f'Sending report:\n{pprint.pformat(operation)}')
        self.service_api.services().report(
            serviceName=SERVICE_NAME, body={
                'operations': [operation]
            }).execute()


def main(argv):
    """Main entrypoint to start services required for Google Marketplace integration."""
    credentials = service_account.Credentials.from_service_account_info(
        json.loads(os.environ['GOOGLE_CREDENTIALS']))

    try:

        if argv[1] == 'pubsub':
            # Starts service that handles google marketplace pub/sub events.
            def callback(message):
                """Callback for handling Cloud Pub/Sub messages."""
                MessageHandler(message, credentials).handle()

            subscriber = pubsub_v1.SubscriberClient(credentials=credentials)
            subscription_path = subscriber.subscription_path(PROJECT_ID,
                                                             PUBSUB_SUBSCRIPTION)
            subscription = subscriber.subscribe(subscription_path, callback=callback)
            print('Listening for messages on {}'.format(subscription_path))
            print('Exit with Ctrl-\\')
            subscription.result()

        elif argv[1] == 'usage':
            # Starts service that sends a marketplace usage report every hour.
            usage_reporter = UsageReporter(credentials=credentials)
            while True:
                print('Sending usage report to marketplace.')
                usage_reporter.report()
                print('Usage report successfully sent. Sleeping for 1 hour.')
                sleep(60 * 60)

    except KeyboardInterrupt:
        print('Program terminated by user. Goodbye.')
        return


if __name__ == '__main__':
    main(sys.argv)
