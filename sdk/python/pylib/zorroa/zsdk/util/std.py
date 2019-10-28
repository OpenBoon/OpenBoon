import datetime
import logging
import os

from importlib import import_module

from pytz import reference

logger = logging.getLogger(__name__)


def str_time_now():
    """Return an Archivist compatible string representation of the current time.

    The format of this value is "%Y-%m-%d %H:%M:%S %z"

    Returns:
        :obj:`datetime.datetime`: A datetime with the given format.

    """
    return datetime.datetime.now(reference.LocalTimezone()).strftime(
        "%Y-%m-%d %H:%M:%S %z")


def as_collection(value):
    """If the given value is not a collection of some type, return
    the value wrapped in a list.

    Args:
        value (:obj:`mixed`):

    Returns:
        :obj:`list` of :obj:`mixed`: The value wrapped in alist.

    """
    if isinstance(value, (set, list, tuple, dict)):
        return value
    return [value]


def import_class(dot_path):
    """Dynamically imports a Python class given a dot-notation style path to
    the class.

    Args:
        dot_path (str): Dot path to a class.

    Returns:
        Class: Class that was imported.

    """
    module_path, class_name = dot_path.rsplit('.', 1)
    module = import_module(module_path)
    return getattr(module, class_name)


def import_and_instantiate(dot_path, *args, **kwargs):
    """Dynamically imports and instantiates a Class.

    Args:
        dot_path (str): Dot path to a class.
        args: Arguments to instantiate the class with.
        kwargs: Kwargs to inbstantiate the class with.

    Returns:
        object: Instance of the class described by the dot_path.

    """
    klass = import_class(dot_path)
    return klass(*args, **kwargs)


def file_exists(path):
    """
    An NFS safe method of testing if a file exists.

    The typical method of checking existence of a file, calling stat() on a file path,
    is not reliable in some NFS environments.  The best way to check if a file actually
    exists or not is to open it.

    Directories fall back to the stat() methodology in os.path.exists which may not
    be NFS safe.  Typically in an NFS environment, you should always attempt to
    make a directory you need, then catch the exception it exists.

    Args:
        path (str): The path to check existence.

    Returns:
        bool: True if the file can be opened, false if not.

    """
    try:
        with open(str(path), "rb") as _:
            pass
        return True
    except IOError as e:
        if e.errno == 21:
            return os.path.exists(path)
    return False
