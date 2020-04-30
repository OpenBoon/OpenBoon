from django.conf import settings
from django.contrib.auth.models import User
from django.core.management import BaseCommand

from apikeys.utils import create_zmlp_api_key
from projects.models import Project, Membership
from subscriptions.models import Subscription
from wallet.utils import get_zmlp_superuser_client


class Command(BaseCommand):
    help = 'Creates a project with a subscription and an admin user.'

    def add_arguments(self, parser):
        parser.add_argument('id', type=str)
        parser.add_argument('name', type=str)
        parser.add_argument('video_hours_limit', type=int)
        parser.add_argument('image_count_limit', type=int)
        parser.add_argument('admin_user', type=str)

    def handle(self, *args, **options):
        superuser = User.objects.get(email='software@zorroa.com')
        project = Project.all_objects.get_or_create(id=options['id'], name=options['name'])[0]
        if not project.is_active:
            project.is_active = True
            project.save()
        project.sync_with_zmlp(superuser)
        try:
            subscription = Subscription.objects.get(project=project)
            subscription.video_hours_limit = options['video_hours_limit']
            subscription.image_count_limit = options['image_count_limit']
        except Subscription.DoesNotExist:
            Subscription.objects.get_or_create(project=project,
                                               video_hours_limit=options['video_hours_limit'],
                                               image_count_limit=options['image_count_limit'])
        user = User.objects.get_or_create(username=options['admin_user'])[0]
        permissions = []
        for role in settings.ROLES:
            permissions += role['permissions']
        roles = [r['name'] for r in settings.ROLES]
        client = get_zmlp_superuser_client(superuser, project_id=str(project.id))
        if not Membership.objects.filter(project=project, user=user).exists():
            membership = Membership(project=project, user=user, roles=roles)
            membership.apikey = create_zmlp_api_key(client, 'new project admin', permissions,
                                                    internal=True)
            membership.save()
