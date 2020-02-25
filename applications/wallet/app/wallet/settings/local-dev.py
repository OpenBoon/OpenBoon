# flake8: noqa
# Settings file intended for use with local development. This
# file overrides the settings to use services that will be available in the docker network
# when using the compose file.
from datetime import timedelta

from wallet.settings import *

# CORS Middleware for handling frontend server requests
# for more customization: https://github.com/adamchainz/django-cors-headers
MIDDLEWARE.insert(0, 'corsheaders.middleware.CorsMiddleware')

# Allow requests from the frontend development server
CORS_ORIGIN_WHITELIST = [
    'http://localhost:8080',
]
CORS_ALLOW_CREDENTIALS = True

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': 'db.sqlite3',
    }
}

ZMLP_API_URL = 'http://localhost:8080'
FQDN = os.environ.get('FQDN', 'http://127.0.0.1:8000')
