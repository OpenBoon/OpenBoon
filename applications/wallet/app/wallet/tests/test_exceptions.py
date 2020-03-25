from zmlp.client import ZmlpSecurityException, ZmlpInvalidRequestException, \
    ZmlpDuplicateException, ZmlpNotFoundException

from wallet.exceptions import zmlp_exception_handler


def test_zmlpsecurityexception_error():
    exc = ZmlpSecurityException({})
    response = zmlp_exception_handler(exc, {})
    assert response.status_code == 403


def test_zmlpinvalidrequestexception_error():
    exc = ZmlpInvalidRequestException({})
    response = zmlp_exception_handler(exc, {})
    assert response.status_code == 400


def test_zmlpnotfoundexception_error():
    exc = ZmlpNotFoundException({})
    response = zmlp_exception_handler(exc, {})
    assert response.status_code == 404


def test_zmlpduplicateexception_error():
    exc = ZmlpDuplicateException({})
    response = zmlp_exception_handler(exc, {})
    assert response.status_code == 409
