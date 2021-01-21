import os
from metrics.settings import *  # noqa


# Uses the postgres database run by the zmlp docker compose env.
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql_psycopg2',
        'NAME': 'metrics',
        'USER': os.environ.get('PG_DB_USER', 'admin'),
        'PASSWORD': os.environ.get('PG_DB_PASSWORD', 'admin'),
        'HOST': os.environ.get('PG_DB_HOST', '127.0.0.1'),
        'PORT': os.environ.get('PG_DB_PORT', '5432'),
    }
}

DEV_DOMAIN = 'https://dev.api.zvi.zorroa.com'
DEV_PIPELINES_KEY = 'eyJhY2Nlc3NLZXkiOiAiTzVBMnVsd3Z6bTlUVmZ2azVyc0VEdyIsICJzZWNyZXRLZXkiOiAiWEZpRWI1V29oWjlFRXkyaVQ5NUhEQSJ9'
