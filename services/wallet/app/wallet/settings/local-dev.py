# flake8: noqa
# Settings file intended for use with local development. This
# file overrides the settings to use services that will be available in the docker network
# when using the compose file.
from datetime import timedelta

from wallet.settings import *

# CORS Middleware for handling frontend server requests
# for more customization: https://github.com/adamchainz/django-cors-headers
MIDDLEWARE.insert(0, 'corsheaders.middleware.CorsMiddleware')

# Remove the CSRF checks for easier API development
MIDDLEWARE.remove('django.middleware.csrf.CsrfViewMiddleware')

# Allow requests from the frontend development server
CORS_ORIGIN_WHITELIST = [
    'http://localhost:8080',
]
CORS_ALLOW_CREDENTIALS = True


REST_FRAMEWORK['DEFAULT_AUTHENTICATION_CLASSES'] = [
    'rest_framework.authentication.BasicAuthentication',
    'rest_framework.authentication.SessionAuthentication',
    'rest_framework_simplejwt.authentication.JWTAuthentication',
]

SIMPLE_JWT = {
'ACCESS_TOKEN_LIFETIME': timedelta(minutes=60)
}