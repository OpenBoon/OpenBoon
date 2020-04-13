# flake8: noqa
# Settings file that connects the runserver to the docker-compose env.
from wallet.settings import *
ZMLP_API_URL = 'https://api.zmlp.zorroa.com'
FQDN = os.environ.get('FQDN', 'http://localhost')

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': 'db.sqlite3',
    }
}
