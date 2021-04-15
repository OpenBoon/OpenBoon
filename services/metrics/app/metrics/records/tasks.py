from celery import Celery
from django.conf import settings
from psqlextra.query import ConflictAction


app = Celery('tasks', broker=f'redis://{settings.REDIS_HOST}:6379/10')
if settings.TESTING:
    app.conf.task_always_eager = True


@app.task
def upsert_api_call(data):
    from metrics.records.models import ApiCall
    upserter = ApiCall.objects.on_conflict(['service', 'asset_id', 'project'],
                                           ConflictAction.UPDATE)
    upserter.insert(**data)
