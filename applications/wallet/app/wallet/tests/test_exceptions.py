from boonsdk.client import BoonSdkSecurityException, BoonSdkInvalidRequestException, \
    BoonSdkDuplicateException, BoonSdkNotFoundException

from wallet.exceptions import zmlp_exception_handler


def test_zmlpsecurityexception_error():
    exc = BoonSdkSecurityException({})
    response = zmlp_exception_handler(exc, {})
    assert response.status_code == 403


def test_zmlpinvalidrequestexception_error():
    exc = BoonSdkInvalidRequestException({})
    response = zmlp_exception_handler(exc, {})
    assert response.status_code == 400


def test_BoonSdkNotFoundException_error():
    exc = BoonSdkNotFoundException({})
    response = zmlp_exception_handler(exc, {})
    assert response.status_code == 404


def test_BoonSdkDuplicateException_error():
    exc = BoonSdkDuplicateException({})
    response = zmlp_exception_handler(exc, {})
    assert response.status_code == 409
