import pytest
from rest_framework.exceptions import PermissionDenied

from wallet.utils import get_zmlp_superuser_client


def test_get_zmlp_superuser_client_no_membership(user):
    with pytest.raises(PermissionDenied):
        get_zmlp_superuser_client(user)
