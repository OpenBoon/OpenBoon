from datetime import date, timedelta

from projects.models import Project
from wallet.celery import app


@app.task
def reap_projects(days_old=30):
    cutoff = date.today() - timedelta(days_old)
    projects_to_delete = Project.all_objects.filter(modifiedDate__lt=cutoff, isActive=False)
    print(f'Beginning deletion of all inactive projects that have not been modified since {cutoff}. '
          f'Found {projects_to_delete.count()} projects to delete.')
    for project in projects_to_delete:
        print(f'Permanently deleting {project.name}:{project.id}.')
        project.hard_delete()
        print(f'Successfully deleted {project.name}:{project.id}.')
