from django.core.management import BaseCommand

from projects.models import Project


class Command(BaseCommand):
    help = 'Deactivates a project.'

    def add_arguments(self, parser):
        parser.add_argument('id', type=str)

    def handle(self, *args, **options):
        try:
            project = Project.objects.get(id=options['id'])
        except Project.DoesNotExist:
            print('Project does not exist.')
            return
        project.is_active = False
        project.save()
