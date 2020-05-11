import json
import pprint
from time import sleep

import sentry_sdk
from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management import BaseCommand
from django.db import transaction
from google.cloud import pubsub_v1
from googleapiclient.errors import HttpError

from apikeys.utils import create_zmlp_api_key
from gcpmarketplace.models import MarketplaceAccount, MarketplaceEntitlement
from gcpmarketplace.utils import get_procurement_api, get_google_credentials
from projects.models import Project, Membership
from subscriptions.models import Subscription
from wallet.utils import get_zmlp_superuser_client

User = get_user_model()

sentry_sdk.init(
    dsn='https://5c1ab0d8be954c35b92283c1290e9924@o280392.ingest.sentry.io/5218609')


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
            'ENTITLEMENT_PLAN_CHANGE_REQUESTED': self._handle_entitlement_plan_changed_request,
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
        entitlement_id = self.payload['entitlement']['id']
        entitlement = self._get_entitlement(entitlement_id)
        if not entitlement:
            print('No entitlement found. Could not handle entitlement creation request.')
            return

        # Wait for the user to be activated.
        account_name = entitlement['account']
        while not MarketplaceAccount.objects.filter(name=account_name).exists():
            print('Waiting for user account to be created. Sleeping for 10 seconds')
            sleep(10)
        user = MarketplaceAccount.objects.get(name=account_name).user
        while not user.is_active:
            print('Waiting for user account to be activated. Sleeping for 10 seconds')
            sleep(10)
            user = User.objects.get(id=user.id)

        if entitlement['state'] == 'ENTITLEMENT_ACTIVATION_REQUESTED':
            name = self._get_entitlement_name(entitlement_id)
            request = get_procurement_api().providers().entitlements().approve(
                name=name, body={})
            request.execute()
            print(f'Approved entitlement {entitlement_id}.')

    def _handle_account_activate(self):
        """Handles the ACCOUNT_ACTIVE event from google marketplace."""

        # Wait for account to exist.
        account_name = (f'providers/DEMO-{settings.MARKETPLACE_PROJECT_ID}/accounts/'  # TODO: remove DEMO when this goes live.
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

        project_name = f'marketplace-{entitlement_id}'
        with transaction.atomic():

            # Create the Project, Subscription and MarketplaceEntitlement.
            project = Project.objects.create(name=project_name)
            project.sync_with_zmlp(self.superuser)
            Subscription.objects.create(project=project, video_hours_limit=100,
                                        image_count_limit=10000)
            entitlement_name = self._get_entitlement_name(entitlement_id)
            MarketplaceEntitlement.objects.create(name=entitlement_name, project=project)

            # Add the account user to the project.
            user = MarketplaceAccount.objects.get(name=entitlement_data['account']).user
            permissions = []
            for role in settings.ROLES:
                permissions += role['permissions']
            roles = [r['name'] for r in settings.ROLES]
            client = get_zmlp_superuser_client(self.superuser, project_id=str(project.id))
            if not Membership.objects.filter(project=project, user=user).exists():
                membership = Membership(project=project, user=user, roles=roles)
                membership.apikey = create_zmlp_api_key(client, 'new project admin',
                                                        permissions,
                                                        internal=True)
                membership.save()

        print(f'Project {project.id} created for entitlement {entitlement_name}.')

    def _handle_entitlement_plan_changed_request(self):
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
        plan_quota_map = {'free': (100, 1000),
                          'decent': (200, 2000),
                          'pretty-good': (300, 3000),
                          'very-good': (400, 4000),
                          'amazing': (500, 5000)}
        entitlement_data = self.payload['entitlement']
        plan_name = entitlement_data['newPlan']
        video_quota, image_quota = plan_quota_map[plan_name]
        entitlement_name = self._get_entitlement_name(entitlement_data['id'])
        subscription = MarketplaceEntitlement.objects.get(name=entitlement_name).project.subscription
        subscription.video_hours_limit = video_quota
        subscription.image_count_limit = image_quota
        subscription.save()
        print(f'Plan changed to {plan_name} for entitlement {entitlement_name}')


    def _handle_entitlement_cancelled(self):
        """Handles the ENTITLEMENT_CANCELLED event from google marketplace."""
        entitlement_name = self._get_entitlement_name(self.payload['entitlement']['id'])
        project = MarketplaceEntitlement.objects.get(name=entitlement_name).project
        project.is_active = False
        project.save()
        print(f'Deactivated project {project.id} for entitlement {entitlement_name}.')


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
        return f'providers/DEMO-{settings.MARKETPLACE_PROJECT_ID}/entitlements/{entitlement_id}'  #TODO: Remove DEMO- when this goes live.


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
