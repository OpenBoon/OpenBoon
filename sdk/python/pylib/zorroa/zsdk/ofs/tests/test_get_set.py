from zorroa.zsdk.ofs import get_ofs, set_ofs
from zorroa.zsdk.ofs.core import AbstractObjectFileSystem
from zorroa.zsdk.ofs.impl import ObjectFileSystem


def test_get_ofs():
    ofs = get_ofs()
    assert isinstance(ofs, AbstractObjectFileSystem)


def test_set_ofs():
    new_ofs = ObjectFileSystem()
    set_ofs(new_ofs)
    assert new_ofs == get_ofs()
