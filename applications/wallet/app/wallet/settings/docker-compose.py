# flake8: noqa
# Settings file that connects the runserver to the docker-compose env.
from wallet.settings import *
ZMLP_API_URL = 'http://localhost:8080'
FQDN = os.environ.get('FQDN', 'http://localhost')
SECURE_HSTS_SECONDS = 0
