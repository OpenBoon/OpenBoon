import os

from zmlp import ZmlpApp


def get_test_file(path):
    """
    Return the path to the given test file.

    Args:
        path (str): The path relative to the test-data directory.

    Returns:
        str: The full absolute file path.
    """
    return os.path.normpath(os.path.join(
        os.path.dirname(__file__),
        '../../../../../../test-data',
        path))


def get_zmlp_app():
    """
    Get a ZmlpApp with a fake key for testing.

    Returns:
        ZmlpApp: An unusable ZMLP app.

    """
    key_dict = {
        'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
        'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
        'sharedKey': 'test123test135'
    }
    return ZmlpApp(key_dict)
