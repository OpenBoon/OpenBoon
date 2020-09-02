import re

from zmlpsdk import ZmlpFatalProcessorException


def flatten_content(content):
    """Recursively flattens list(s) of strings into a single space-delimited string.

    Args:
        content (list or str): List of strings to flatten.

    Returns:
        str: Flattened string of all content.

    """
    if isinstance(content, list):
        return ' '.join(content)
    elif isinstance(content, str):
        return content
    else:
        raise ZmlpFatalProcessorException('input must be list or str')


def remove_parentheticals(content):
    """ Get rid of CC parentheticals

    Args:
        content (str): content string

    Returns:
        same string with parentheticals removed
    """
    return re.sub(r'\[.*?\]', '', content)
