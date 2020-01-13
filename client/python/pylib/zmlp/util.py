import functools
import uuid


def is_valid_uuid(val):
    """
    Return true if the given value is a valid UUID.

    Args:
        val (str): a string which might be a UUID.

    Returns:
        bool: True if UUID

    """
    try:
        uuid.UUID(str(val))
        return True
    except ValueError:
        return False


def as_collection(value):
    """If the given value is not a collection of some type, return
    the value wrapped in a list.

    Args:
        value (:obj:`mixed`):

    Returns:
        :obj:`list` of :obj:`mixed`: The value wrapped in alist.

    """
    if value is None:
        return None
    if isinstance(value, (set, list, tuple, dict)):
        return value
    return [value]


def memoize(func):
    """
    Cache the result of the given function.

    Args:
        func (function): A function to wrap.

    Returns:
        function: a wrapped function
    """
    cache = func.cache = {}

    @functools.wraps(func)
    def memoized_func(*args, **kwargs):
        key = str(args) + str(kwargs)
        if key not in cache:
            cache[key] = func(*args, **kwargs)
        return cache[key]

    return memoized_func
