from organizations.models import Organization


def test_organization_str():
    org = Organization(name='test')
    assert str(org) == 'test'


def test_organization_repr():
    org = Organization(name='test')
    assert repr(org) == "Organization(name='test')"
