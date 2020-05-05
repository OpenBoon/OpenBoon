import json

from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management import BaseCommand
from django.db import transaction

from apikeys.utils import create_zmlp_api_key
from projects.models import Project, Membership
from subscriptions.models import Subscription
from wallet.utils import get_zmlp_superuser_client

User = get_user_model()


class Command(BaseCommand):
    help = 'Returns the last hour of usage for a project in json form.'

    def add_arguments(self, parser):
        parser.add_argument('project_id', type=str)

    def handle(self, *args, **options):
        project = Project.objects.get(id=options['project_id'])
        usage = project.subscription.usage_last_hour()
        return json.dumps(usage)
