from datetime import timedelta, date
from time import sleep

from django.core.management import BaseCommand

from projects.models import Project


def reap_projects(days_old=30):
    cutoff = date.today() - timedelta(days_old)
    projects_to_delete = Project.all_objects.filter(modifiedDate__lt=cutoff, isActive=False)
    print(f'Beginning deletion of all inactive projects that have not been modified since {cutoff}. '
          f'Found {projects_to_delete.count()} projects to delete.')
    for project in projects_to_delete:
        print(f'Permanently deleting {project.name}:{project.id}.')
        project.hard_delete()
        print(f'Successfully deleted {project.name}:{project.id}.')


class Command(BaseCommand):
    help = 'Hard deletes any projects that have been inactive for more than 30 days.'

    def handle(self, *args, **options):
        while True:
            reap_projects()
            sleep(60 * 60 * 24)  # Sleep for 1 day.
