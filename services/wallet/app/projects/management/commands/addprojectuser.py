import base64

from django.contrib.auth.models import User
from django.core.management.base import BaseCommand
from django.db import transaction
from pathlib2 import Path

from projects.models import Project


class Command(BaseCommand):
    help = 'Creates a project user and adds their ZMLP Api Key'

    def add_arguments(self, parser):
        parser.add_argument('username', type=str)
        parser.add_argument('project', type=str)
        parser.add_argument('key_file', type=str)

    def handle(self, *args, **options):
        apikey = base64.b64encode(
            Path(options['key_file']).expanduser().read_bytes()).decode('utf-8')
        user = User.objects.get(username=options['username'])
        with transaction.atomic():
            project = Project.objects.get_or_create(id=options['project'])[0]
            project.users.add(user, through_defaults={'apikey': apikey})
