"""
Django settings for wallet project.

Generated by 'django-admin startproject' using Django 2.1.

For more information on this file, see
https://docs.djangoproject.com/en/2.1/topics/settings/

For the full list of settings and their values, see
https://docs.djangoproject.com/en/2.1/ref/settings/
"""

import os

import sentry_sdk
from django.http import JsonResponse
from rest_framework import status
from sentry_sdk.integrations.django import DjangoIntegration

VERSION = '0.1.0'
ENVIRONMENT = os.environ.get('ENVIRONMENT', 'localdev')
FQDN = os.environ.get('FQDN', 'http://localhost')

if os.environ.get('ENABLE_SENTRY', 'false').lower() == 'true':
    # Sentry Configuration
    sentry_sdk.init(
        dsn="https://d772538aae2649d38a8931583ed7719b@sentry.io/1504338",
        integrations=[DjangoIntegration()],
        release=f'wallet-{VERSION}',
        environment=ENVIRONMENT,
        send_default_pii=True
    )

# Build paths inside the project like this: os.path.join(BASE_DIR, ...)
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

REACT_APP_DIR = os.path.join(BASE_DIR, '..', 'frontend')

# Quick-start development settings - unsuitable for production
# See https://docs.djangoproject.com/en/2.1/howto/deployment/checklist/

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = '4*-c#z+_^gwef_ai&!5vfxf_al_#o^lx(4u70@q#n057a&65j$'

# SECURITY WARNING: don't run with debug turned on in production!
if os.environ.get('DEBUG') == 'true':
    DEBUG = True
else:
    DEBUG = False

ALLOWED_HOSTS = ['*']


# Application definition

INSTALLED_APPS = [
    'agreements',
    'gcpmarketplace',
    'jobs',
    'modules',
    'organizations',
    'projects',
    'registration',
    'searches',
    'subscriptions',
    'supportadmin',
    'wallet',
    'webhooks',
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'django.contrib.postgres',
    'axes',
    'rest_framework',
    'corsheaders',
    'health_check',
    'health_check.db',
    'health_check.cache',
    'health_check.storage',
    'rest_auth'
]

MIDDLEWARE = [
    # We should ask a security consultant/auditor if this is going to expose us to
    # potential BREACH attacks - (http://breachattack.com/)
    'django.middleware.gzip.GZipMiddleware',
    'django.middleware.security.SecurityMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',

    # AxesMiddleware should be the last middleware in the MIDDLEWARE list.
    # It only formats user lockout messages and renders Axes lockout responses
    # on failed user authentication attempts from login views.
    # If you do not want Axes to override the authentication response
    # you can skip installing the middleware and use your own views.
    'axes.middleware.AxesMiddleware',
]

ROOT_URLCONF = 'wallet.urls'

TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [],
        'APP_DIRS': True,
        'OPTIONS': {
            'context_processors': [
                'django.template.context_processors.debug',
                'django.template.context_processors.request',
                'django.contrib.auth.context_processors.auth',
                'django.contrib.messages.context_processors.messages',
            ],
        },
    },
]

WSGI_APPLICATION = 'wallet.wsgi.application'

# Database
# https://docs.djangoproject.com/en/2.1/ref/settings/#databases

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': 'wallet',
        'USER': 'wallet',
        'PASSWORD': os.environ.get('PG_PASSWORD', 'a8fnnbe934j'),
        'HOST': os.environ.get('PG_HOST', 'localhost'),
        'PORT': '5432',
    }
}

# Password validation
# https://docs.djangoproject.com/en/2.1/ref/settings/#auth-password-validators

AUTH_PASSWORD_VALIDATORS = [
    {
        'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator',
    },
]


# Add the django-axes authentication backend and configure.
def axes_lockout_callable(request, credentials):
    message = ('This account has been locked due to too many failed login attempts. '
               'Please contact support to unlock your account.')
    return JsonResponse(data={'detail': [message]}, status=status.HTTP_423_LOCKED)


AXES_LOCKOUT_CALLABLE = axes_lockout_callable
AXES_META_PRECEDENCE_ORDER = ['HTTP_X_FORWARDED_FOR']
AXES_FAILURE_LIMIT = 5
AUTHENTICATION_BACKENDS = [
    # AxesBackend should be the first backend in the AUTHENTICATION_BACKENDS list.
    'axes.backends.AxesBackend',

    # Django ModelBackend is the default authentication backend.
    'django.contrib.auth.backends.ModelBackend',
]

# Internationalization
# https://docs.djangoproject.com/en/2.1/topics/i18n/

LANGUAGE_CODE = 'en-us'

TIME_ZONE = 'UTC'

USE_I18N = True

USE_L10N = True

USE_TZ = True


# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/2.1/howto/static-files/
# The static files are built in the static_server directory in the root of the repo.
# These files are then built into the nginx static file server container.
STATIC_URL = '/static/'
STATIC_ROOT = os.path.join(BASE_DIR, 'static')


# Rest Framework Specific Settings
REST_FRAMEWORK = {
    'DEFAULT_PAGINATION_CLASS': 'wallet.paginators.FromSizePagination',
    'DEFAULT_RENDERER_CLASSES': ['rest_framework.renderers.JSONRenderer'],
    # The Frontend currently assumes a page size of 20 - notify them if this value changes.
    'PAGE_SIZE': 20,
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'rest_framework.authentication.SessionAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAuthenticated',
    ],

    # IMPORTANT: Only accepting JSON is required to mitigate CSRF attacks on unauthenticated
    # POST endpoints. Do not add additional parsers.
    'DEFAULT_PARSER_CLASSES': [
        'rest_framework.parsers.JSONParser',
    ],

    'TEST_REQUEST_DEFAULT_FORMAT': 'json',
    'EXCEPTION_HANDLER': 'wallet.exceptions.zmlp_exception_handler',
    'JSON_UNDERSCOREIZE': {
        'no_underscore_before_number': True,
    }
}

BROWSABLE = os.environ.get('BROWSABLE')
if BROWSABLE == 'true':
    REST_FRAMEWORK['DEFAULT_RENDERER_CLASSES'].append(
        'rest_framework.renderers.BrowsableAPIRenderer')

REST_AUTH_SERIALIZERS = {
    'PASSWORD_RESET_SERIALIZER': 'registration.serializers.PasswordResetSerializer'
}

# General Application Configuration
BOONAI_API_URL = os.environ.get('BOONAI_API_URL', 'archivist')
INCEPTION_KEY_B64 = os.environ.get('INCEPTION_KEY_B64')
METRICS_API_URL = os.environ.get('METRICS_API_URL', 'http://metrics')

# Google OAUTH2
GOOGLE_OAUTH_CLIENT_ID = os.environ.get(
    'GOOGLE_OAUTH_CLIENT_ID',
    ''
) + '.apps.googleusercontent.com'

# Mail Server
DEFAULT_FROM_EMAIL = 'do_not_reply@boonai.io'
EMAIL_HOST = 'smtp.mailgun.org'
EMAIL_HOST_USER = 'postmaster@mg.boonai.io'
EMAIL_HOST_PASSWORD = os.environ.get('SMTP_PASSWORD')
EMAIL_PORT = 587
EMAIL_USE_TLS = True
OLD_PASSWORD_FIELD_ENABLED = True

# Django Registration Settings
REGISTRATION_TIMEOUT_DAYS = 3  # Numbers of days the confirmation link is valid.

# Session Settings
SESSION_COOKIE_AGE = 28800
# Allows the Session Cookie Age to essentially function as the idle timeout
SESSION_SAVE_EVERY_REQUEST = True

# Logging Settings
LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'handlers': {
        'console': {
            'class': 'logging.StreamHandler',
        },
    },
    'loggers': {
        'django': {
            'handlers': ['console'],
            'level': os.getenv('DJANGO_LOG_LEVEL', 'INFO'),
        },
    },
}

# Add Cross Origin support for Frontend developers running against staging
if ENVIRONMENT == 'staging':
    # CORS Middleware for handling frontend server requests
    # for more customization: https://github.com/adamchainz/django-cors-headers
    # Inserted after the GZIP middleware
    MIDDLEWARE.insert(1, 'corsheaders.middleware.CorsMiddleware')
    # Allow requests from the frontend development server
    CORS_ORIGIN_WHITELIST = [
        'http://localhost:3000',
    ]
    CORS_ALLOW_CREDENTIALS = True

# Roles & Permissions Mapping
# Each Role is required to have at least one permission assigned to it.
ROLES = [
    {'name': 'ML_Tools',
     'description': 'Provides access to the Job Queue, Data Sources, and Visualizer.',
     'permissions': ['AssetsRead', 'AssetsImport', 'AssetsDelete', 'DataSourceManage',
                     'DataQueueManage']},
    {'name': 'API_Keys',
     'description': 'Provides access to API Key provisioning.',
     'permissions': ['ProjectManage']},
    {'name': 'User_Admin',
     'description': 'Allows adding and removing users as well as managing their roles. This '
                    'includes adding and removing their own roles.',
     'permissions': ['ProjectManage']},
    {'name': 'Webhooks',
     'description': 'Allows creating, editing and deleting webhooks.',
     'permissions': ['WebhooksCreate']} # TODO: Add the correct permissions.
]

# The registered email address of the superuser for this instance.
SUPERUSER_EMAIL = 'software@zorroa.com'

# If true a full featured django admin page is available.
SUPERADMIN = os.environ.get('SUPERADMIN') == 'true'

# Google Marketplace Integration Settings
MARKETPLACE_PROJECT_ID = os.environ.get('MARKETPLACE_PROJECT_ID')
MARKETPLACE_PUBSUB_SUBSCRIPTION = os.environ.get('MARKETPLACE_PUBSUB_SUBSCRIPTION')
MARKETPLACE_SERVICE_NAME = os.environ.get('MARKETPLACE_SERVICE_NAME')
MARKETPLACE_CREDENTIALS = os.environ.get('MARKETPLACE_CREDENTIALS')

FEATURE_FLAGS = {
    'USE_MODEL_IDS_FOR_LABEL_FILTERS': os.environ.get('USE_MODEL_IDS_FOR_LABEL_FILTERS', 'false') == 'true'  # noqa
}

# Security Settings
SECURE_HSTS_SECONDS = 31536000
SECURE_CONTENT_TYPE_NOSNIFF = True
