from celery import Celery
from django.conf import settings

app = Celery('tasks', broker=f'redis://{settings.REDIS_HOST}:6379/10')


@app.task
def save_serializer(data):
    from metrics.records.serializers import ApiCallSerializer
    serializer = ApiCallSerializer(data=data)
    serializer.is_valid(raise_exception=True)
    serializer.save()
