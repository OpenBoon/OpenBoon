import os

from boonsdk import BoonSdkApp


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


def get_boon_app():
    """
    Get a BoonSdkApp with a fake key for testing.

    Returns:
        BoonSdkApp: An unusable Boon AI app.

    """
    key_dict = {
        'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
        'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
        'sharedKey': 'test123test135'
    }
    return BoonSdkApp(key_dict)
