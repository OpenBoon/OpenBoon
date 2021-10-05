import json
import pprint
from time import sleep

from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management import BaseCommand
from django.db import transaction
from google.cloud import pubsub_v1
from googleapiclient.errors import HttpError

from gcpmarketplace.models import MarketplaceAccount, MarketplaceEntitlement
from gcpmarketplace.utils import get_procurement_api, get_google_credentials
from organizations.models import Organization

User = get_user_model()


class MessageHandler(object):
    """Handles pub sub messages from the gcp marketplace.

    Args:
        message(pubsub_v1.subscriber.message.Message): Message to handle.

    """
    superuser = User.objects.get(email=settings.SUPERUSER_EMAIL)

    def __init__(self, message):
        self.message = message
        self.payload = json.loads(message.data)
        self.event_type = self.payload['eventType']

    def handle(self):
        """Handle a pub/sub message send by the gcp marketplace."""

        # For information on what these events mean view the gcp marketplace docs.
        # https://cloud.google.com/marketplace/docs/partners/integrated-saas/backend-integration#eventtypes  # noqa
        event_map = {
            'ENTITLEMENT_CREATION_REQUESTED': self._handle_entitlement_creation_requested,
            'ACCOUNT_ACTIVE': self._handle_account_activate,
            'ENTITLEMENT_ACTIVE': self._handle_entitlement_active,
            'ENTITLEMENT_PLAN_CHANGE_REQUESTED': self._handle_entitlement_plan_changed_requested,
            'ENTITLEMENT_PLAN_CHANGED': self._handle_entitlement_plan_changed,
            'ENTITLEMENT_CANCELLED': self._handle_entitlement_cancelled}

        if self.event_type in event_map:
            print(f'Handling {self.event_type} Event \nPayload: {pprint.pformat(self.payload)}')
            event_map[self.event_type]()
        else:
            print(f'Ignoring unknown event type {self.event_type}: {pprint.pformat(self.payload)}.')
        self.message.ack()

    def _handle_entitlement_creation_requested(self):
        """Handles the ENTITLEMENT_CREATION_REQUESTED event from google marketplace."""
        def continue_waiting_for_account():
            """Closure used to determine if the outer function should continue to wait
            for an account to be active. Returns True if the function should continue to
            wait. In the event of a timeout the closure handles canceling the entitlement
            and returns False.

            """
            nonlocal account_timer
            nonlocal sleep_time
            nonlocal entitlement
            account_timer += sleep_time
            if account_timer >= (60 * 60 * 2):  # 2 hours
                print(f'Account {account_name} has been waiting for activation for '
                      f'2 hours. Timing out and moving on.')
                if entitlement['state'] == 'ENTITLEMENT_ACTIVATION_REQUESTED':
                    print(f'Due to account activation timeout cancelling '
                          f'entitlement {entitlement_id}')
                    reason = ('You did not complete account activation within 2 hours. '
                              'Please try again.')
                    request = get_procurement_api().providers().entitlements().reject(
                        name=entitlement["name"], body={'reason': reason})
                    request.execute()
                return False
            entitlement = self._get_entitlement(entitlement_id)
            if entitlement['state'] != 'ENTITLEMENT_ACTIVATION_REQUESTED':
                return False
            return True

        entitlement_id = self.payload['entitlement']['id']
        entitlement = self._get_entitlement(entitlement_id)
        if not entitlement:
            print('No entitlement found. Could not handle entitlement creation request.')
            return

        # Check entitlement state.
        if entitlement['state'] != 'ENTITLEMENT_ACTIVATION_REQUESTED':
            print(f'Entitlement {entitlement_id} is in the {entitlement["state"]} state. '
                  f'Ignoring this request and moving on.')
            return

        # Wait for the user to be activated.
        account_name = entitlement['account']
        account_timer = 0
        sleep_time = 10
        while not MarketplaceAccount.objects.filter(name=account_name).exists():
            print(f'Waiting for user account {account_name} to be created. Sleeping for 10 seconds')
            sleep(sleep_time)
            if not continue_waiting_for_account():
                return
        user = MarketplaceAccount.objects.get(name=account_name).user
        while not user.is_active:
            print('Waiting for user account {account_name} to be activated. '
                  'Sleeping for 10 seconds')
            sleep(sleep_time)
            if not continue_waiting_for_account():
                return
            user = User.objects.get(id=user.id)

        # Approve the entitlement.
        name = self._get_entitlement_name(entitlement_id)
        request = get_procurement_api().providers().entitlements().approve(
            name=name, body={})
        request.execute()
        print(f'Approved entitlement {entitlement_id}.')

    def _handle_account_activate(self):
        """Handles the ACCOUNT_ACTIVE event from google marketplace."""

        # Wait for account to exist.
        account_name = (f'providers/{settings.MARKETPLACE_PROJECT_ID}/accounts/'
                        f'{self.payload["account"]["id"]}')
        while not MarketplaceAccount.objects.filter(name=account_name).exists():
            print('Waiting for user account to be created. Sleeping for 10 seconds')
            sleep(10)

        # Activate the user.
        account = MarketplaceAccount.objects.get(name=account_name)
        user = account.user
        if not user.is_active:
            user.is_active = True
            user.save()
        print(f'Account {account.name} is active.')

    def _handle_entitlement_active(self):
        """Handles the ENTITLEMENT_ACTIVE event from google marketplace."""
        entitlement_id = self.payload['entitlement']['id']
        entitlement_data = self._get_entitlement(entitlement_id)

        with transaction.atomic():

            # Create the Organization, Subscription and MarketplaceEntitlement.
            user = MarketplaceAccount.objects.get(name=entitlement_data['account']).user
            organization = Organization.objects.create(owner=user, plan=entitlement_data['plan'])
            entitlement_name = self._get_entitlement_name(entitlement_id)
            MarketplaceEntitlement.objects.create(name=entitlement_name, organization=organization)

        print(f'Organization {organization.id} created for entitlement {entitlement_name}.')

    def _handle_entitlement_plan_changed_requested(self):
        """Handles the ENTITLEMENT_PLAN_CHANGED_REQUESTED event from google marketplace."""
        entitlement = self._get_entitlement(self.payload['entitlement']['id'])
        if entitlement['state'] == 'ENTITLEMENT_PENDING_PLAN_CHANGE_APPROVAL':
            body = {'pendingPlanName': entitlement['newPendingPlan']}
            request = get_procurement_api().providers().entitlements().approvePlanChange(
                name=entitlement['name'], body=body)
            request.execute()
            print(f'Approved plan change for entitlement {entitlement["name"]} to '
                  f'{entitlement["newPendingPlan"]} plan.')
        else:
            print(f'Entitlement {entitlement["name"]} state was not '
                  f'ENTITLEMENT_PENDING_PLAN_CHANGE_APPROVAL so the event was ignored.')

    def _handle_entitlement_plan_changed(self):
        """Handles the ENTITLEMENT_PLAN_CHANGED event from google marketplace."""
        entitlement_data = self.payload['entitlement']
        plan_name = entitlement_data['newPlan']
        entitlement_name = self._get_entitlement_name(entitlement_data['id'])
        organization = MarketplaceEntitlement.objects.get(name=entitlement_name).organization
        organization.plan = plan_name
        organization.save()
        print(f'Plan changed to {plan_name} for entitlement {entitlement_name}')

    def _handle_entitlement_cancelled(self):
        """Handles the ENTITLEMENT_CANCELLED event from google marketplace."""
        entitlement_name = self._get_entitlement_name(self.payload['entitlement']['id'])
        try:
            organization = MarketplaceEntitlement.objects.get(name=entitlement_name).organization
        except MarketplaceEntitlement.DoesNotExist:
            print(f'No record of entitlement {entitlement_name} existed. Cancellation complete.')
            return
        organization.isActive = False
        organization.save()
        print(f'Deactivated organization {organization.id} for entitlement {entitlement_name}.')

    def _get_entitlement(self, entitlement_id):
        """Gets an entitlement from the Procurement Service.

        Args:
            entitlement_id(str): UUID of the entitlement to retrieve.

        Returns(dict): Dictionary representing an entitlement.

        """
        name = self._get_entitlement_name(entitlement_id)
        request = get_procurement_api().providers().entitlements().get(name=name)
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
        # TODO: Remove DEMO- when this goes live.
        return f'providers/{settings.MARKETPLACE_PROJECT_ID}/entitlements/{entitlement_id}'


class Command(BaseCommand):
    help = 'Starts service that handles google marketplace pub/sub events.'

    def handle(self, *args, **options):

        def callback(message):
            """Callback for handling Cloud Pub/Sub messages."""
            MessageHandler(message).handle()

        subscriber = pubsub_v1.SubscriberClient(credentials=get_google_credentials())
        subscription_path = subscriber.subscription_path(settings.MARKETPLACE_PROJECT_ID,
                                                         settings.MARKETPLACE_PUBSUB_SUBSCRIPTION)
        subscription = subscriber.subscribe(subscription_path, callback=callback)
        print('Listening for messages on {}'.format(subscription_path))
        print('Exit with Ctrl-\\')
        try:
            subscription.result()
        except KeyboardInterrupt:
            print('Program terminated by user. Goodbye.')
            return
