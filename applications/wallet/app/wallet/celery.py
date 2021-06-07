import os

from celery import Celery
from django.conf import settings

# Set the default Django settings module for the 'celery' program.
from django_celery_beat.models import IntervalSchedule, PeriodicTask

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'wallet.settings')

app = Celery('wallet', broker=f'redis://{settings.REDIS_HOST}:6379/20')

# Using a string here means the worker doesn't have to serialize
# the configuration object to child processes.
# - namespace='CELERY' means all celery-related configuration keys
#   should have a `CELERY_` prefix.
app.config_from_object('django.conf:settings', namespace='CELERY')

# Load task modules from all registered Django apps.
app.autodiscover_tasks()

# Set up celery beat tasks.
schedule, created = IntervalSchedule.objects.get_or_create(every=1, period=IntervalSchedule.DAYS,)
PeriodicTask.objects.get_or_create(interval=schedule, name='Project Reaper',
                                   task='projects.tasks.reap_projects')
