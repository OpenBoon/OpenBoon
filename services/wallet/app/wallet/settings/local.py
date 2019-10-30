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

MIDDLEWARE.append('wallet.middleware.local_dev_cors_middleware')
