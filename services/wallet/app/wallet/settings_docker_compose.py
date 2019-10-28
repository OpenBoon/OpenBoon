# Settings file intended for use with docker-compose.yml in the root of the repo. This
# file overrides the settings to use services that will be available in the docker network
# when using the compose file.
from .settings import *  # noqa

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': 'wallet',
        'USER': 'admin',
        'PASSWORD': 'a8fnnbe934j',
        'HOST': 'postgres',
        'PORT': '5432',
    }
}
