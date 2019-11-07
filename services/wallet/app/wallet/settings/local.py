# Settings file intended for use with docker-compose.yml in the root of the repo. This
# file overrides the settings to use services that will be available in the docker network
# when using the compose file.
import os

from wallet.settings import *  # noqa

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': os.path.join(BASE_DIR, 'db.sqlite3')
    }
}

# CORS Middleware for handling frontend server requests
# for more customization: https://github.com/adamchainz/django-cors-headers
MIDDLEWARE.insert(0, 'corsheaders.middleware.CorsMiddleware')
CORS_ORIGIN_WHITELIST = [
    'http://localhost:8080'
]
CORS_ALLOW_CREDENTIALS = True


REST_FRAMEWORK['DEFAULT_AUTHENTICATION_CLASSES'] = [
    'rest_framework.authentication.BasicAuthentication',
    'rest_framework.authentication.SessionAuthentication',
    'rest_framework_simplejwt.authentication.JWTAuthentication',
]