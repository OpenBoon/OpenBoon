from uuid import uuid4

from django.contrib.auth.models import User

from registration.models import UserRegistrationToken


def test_user_registration_token_str():
    _id = uuid4()
    token = UserRegistrationToken(user=User(username='bud'), token=_id)
    assert str(token) == f'bud: {_id}'
