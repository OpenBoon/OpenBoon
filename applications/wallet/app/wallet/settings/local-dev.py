# flake8: noqa
# Settings file intended for use with local development. This file is used by pytest to run local
# tests.
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

BOONAI_API_URL = 'http://localhost:8080'
FQDN = os.environ.get('FQDN', 'http://127.0.0.1:8000')
SECURE_HSTS_SECONDS = 0

# Unsafe password hasher used here to drastically speed up tests.
PASSWORD_HASHERS = [
    'django.contrib.auth.hashers.MD5PasswordHasher',
]
