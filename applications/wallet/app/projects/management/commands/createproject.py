from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management import BaseCommand
from django.db import transaction

from apikeys.utils import create_zmlp_api_key
from projects.models import Project, Membership
from wallet.utils import get_zmlp_superuser_client

User = get_user_model()


class Command(BaseCommand):
    help = 'Creates a project with an admin user.'

    def add_arguments(self, parser):
        parser.add_argument('id', type=str)
        parser.add_argument('name', type=str)
        parser.add_argument('admin_user', type=str)

    def handle(self, *args, **options):
        with transaction.atomic():
            project = Project.all_objects.get_or_create(id=options['id'], name=options['name'])[0]
            client = get_zmlp_superuser_client(project_id=str(project.id))
            if not project.isActive:
                project.isActive = True
                project.save()
            project.sync_with_zmlp()
            user = User.objects.get_or_create(username=options['admin_user'])[0]
            permissions = []
            for role in settings.ROLES:
                permissions += role['permissions']
            roles = [r['name'] for r in settings.ROLES]

            if not Membership.objects.filter(project=project, user=user).exists():
                membership = Membership(project=project, user=user, roles=roles)
                membership.apikey = create_zmlp_api_key(client, 'new project admin', permissions,
                                                        internal=True)
                membership.save()
