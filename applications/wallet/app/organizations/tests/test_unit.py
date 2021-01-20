from organizations.models import Organization


def test_organization_str():
    org = Organization(name='test', owner_id=1)
    assert str(org) == 'test'


def test_organization_repr():
    org = Organization(name='test', owner_id=1)
    assert repr(org) == "Organization(name='test', owner_id=1)"
