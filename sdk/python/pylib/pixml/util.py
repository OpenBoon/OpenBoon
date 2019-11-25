
from importlib import import_module

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

