# flake8: noqa
# Settings file that connects the runserver to the docker-compose env.
from wallet.settings import *
BOONAI_API_URL = 'http://localhost:8080'
FQDN = os.environ.get('FQDN', 'http://localhost')
SECURE_HSTS_SECONDS = 0
INCEPTION_KEY_B64 = 'ewogICAgImFjY2Vzc0tleSI6ICJSRGMyTmtFeVFVWXRRakJHUXkwMFFVVkVMVGsxTVRZdE9UVTBOME5DTXpOQ05rTTJDZyIsCiAgICAic2VjcmV0S2V5IjogInBjZWtqRFZfaXBTTVhBYUJxcXRxNkp3eTVGQU1uamVoVVFyTUVoYkc4VzAxZ2lWcVZMZkVOOUZkTUl2enUwcmIiCn0KCg=='
DEBUG = 'true'
BROWSABLE = 'true'
SUPERADMIN = 'true'
