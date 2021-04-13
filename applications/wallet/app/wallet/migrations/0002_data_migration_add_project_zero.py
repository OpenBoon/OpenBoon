# Generated by Django 2.2.12 on 2020-05-20 18:17

import logging

import backoff
import requests
from django.conf import settings
from django.contrib.auth import get_user_model
from django.db import migrations

from projects.models import Project, Membership

User = get_user_model()
logger = logging.getLogger(__name__)


def create_project_zero(apps, schema_editor):
    # Create project zero (or get it for deployments where it may already exist).
    Organization = apps.get_model('organizations', 'Organization')
    org, created = Organization.all_objects.get_or_create(name='Boon AI')
    project_zero, created = Project.objects.get_or_create(id='00000000-0000-0000-0000-000000000000',
                                                          name='Project Zero',
                                                          organization_id=org.id)
    project_zero.save()

    # Create the membership if it doesn't already exist
    user = User.objects.get(username=settings.SUPERUSER_EMAIL)
    membership = Membership.objects.get_or_create(user=user, project=project_zero,
                                                  roles=[r['name'] for r in settings.ROLES])[0]

    # Sync Project Zero to Zmlp
    sync_project(project_zero, membership)


@backoff.on_exception(backoff.expo, requests.exceptions.ConnectionError, max_time=300)
def sync_project(project_zero, membership):
    try:
        project_zero.sync_with_zmlp()
        membership.sync_with_zmlp(project_zero.get_zmlp_super_client())
    except Exception:
        raise requests.exceptions.ConnectionError()


class Migration(migrations.Migration):

    dependencies = [
        ('wallet', '0001_data_migration_add_superuser'),
        ('projects', '0011_auto_20210410_0146'),
    ]

    operations = [
        migrations.RunPython(create_project_zero),
    ]

