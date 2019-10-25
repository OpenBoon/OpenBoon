from .impl import ObjectFileSystem

"""
A global OFS instance.
"""
__ofs = ObjectFileSystem()


def get_ofs():
    """
    Get the global OFS instance.

    Returns:
        ObjectFileSystem: The global OFS instance.

    """
    return __ofs


def set_ofs(ofs_impl):
    """
    Initialize and replace the global OFS instance with the supplied instance. This
    is mainly used for testing.

    Args:
        ofs_impl (ObjectFileSystem): A ObjectFileSystem instance.

    """
    global __ofs
    __ofs = ofs_impl
    __ofs.init()
