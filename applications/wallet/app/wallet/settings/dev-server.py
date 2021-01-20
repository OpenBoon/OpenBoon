from wallet.settings import *  # noqa

# Points to the Live Development Servers ZMLP API
ZMLP_API_URL = 'https://dev.api.zvi.zorroa.com'

# Useful for local debugging
DEBUG = True

# Turns on the browsable API for your local runserver
BROWSABLE = True

SUPERADMIN = True

# Required for authenticating requests with the ZMLP API
INCEPTION_KEY_B64 = 'ewogICAgIm5hbWUiOiAiYWRtaW4ta2V5IiwKICAgICJwcm9qZWN0SWQiOiAiMDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAwIiwKICAgICJpZCI6ICJmM2JkMjU0MS00MjhkLTQ0MmItOGExNy1lNDAxZTVlNzZkMDYiLAogICAgImFjY2Vzc0tleSI6ICJoMnZVTklNOGpLbFZmSEczMGUxc2FodTBjVzU5ME1qV3l5NHpTajRSelNzd3pyc3hWQSIsCiAgICAic2VjcmV0S2V5IjogImh4czRtNUh1R0dRUU1IenUxY0FZS2hiOUtoTUY0Q0ZxZ29hWmhjRVJlM3o5bEttY3Zxb21MaEJiV1o4c1FmcUUiLAogICAgInBlcm1pc3Npb25zIjogWwogICAgICAgICJQcm9qZWN0RmlsZXNXcml0ZSIsICJTeXN0ZW1Qcm9qZWN0RGVjcnlwdCIsICJTeXN0ZW1NYW5hZ2UiLCAiU3lzdGVtUHJvamVjdE92ZXJyaWRlIiwgIkFzc2V0c0ltcG9ydCIsICJTeXN0ZW1Nb25pdG9yIiwgIlByb2plY3RNYW5hZ2UiLCAiUHJvamVjdEZpbGVzUmVhZCIsICJBc3NldHNSZWFkIiwgIkFzc2V0c0RlbGV0ZSIKICAgIF0KfQo='

# Required to use the Live Development Servers Postgres DB
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': 'wallet',
        'USER': 'wallet',
        'PASSWORD': 'tfH2TI9RvnTYtyWV',
        'HOST': 'localhost',
        'PORT': '5432',
    }
}

# Mail settings
EMAIL_HOST_PASSWORD = '8df48265cfe3e9f2995d6ca2006af5ec-52b6835e-6d2c733d'

ENVIRONMENT = 'zvi-dev'
FQDN = 'https://dev.console.zvi.zorroa.com'


# Turn off Migrations for the runserver
class DisableMigrations():
    def __contains__(self, item):
        return True

    def __getitem__(self, item):
        return None


MIGRATION_MODULES = DisableMigrations()
