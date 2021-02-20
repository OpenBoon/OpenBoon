from wallet.settings import *  # noqa

# Points to the Live Development Servers ZMLP API
BOONAI_API_URL = 'https://dev.api.boonai.app'

# Useful for local debugging
DEBUG = True

# Turns on the browsable API for your local runserver
REST_FRAMEWORK['DEFAULT_RENDERER_CLASSES'].append('rest_framework.renderers.BrowsableAPIRenderer')  # noqa

SUPERADMIN = True

# Required for authenticating requests with the ZMLP API
INCEPTION_KEY_B64 = 'Replace with the Dev Servers value in the Wallet Service YAML file'

# Required to use the Live Development Servers Postgres DB
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': 'wallet',
        'USER': 'wallet',
        'PASSWORD': 'Replace with the Dev Servers value in the Wallet Service YAML file',
        'HOST': 'localhost',
        'PORT': '5432',
    }
}

# Mail settings
EMAIL_HOST_PASSWORD = 'Replace with the Dev Servers value in the Wallet Service YAML file'

ENVIRONMENT = 'dev'
FQDN = 'https://dev.boonai.app'

# Turn off Migrations for the runserver


class DisableMigrations():
    def __contains__(self, item):
        return True

    def __getitem__(self, item):
        return None


MIGRATION_MODULES = DisableMigrations()
