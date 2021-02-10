import os

from boonai import BoonAiApp


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


def get_boonai_app():
    """
    Get a BoonAiApp with a fake key for testing.

    Returns:
        BoonAiApp: An unusable ZMLP app.

    """
    key_dict = {
        'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
        'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
        'sharedKey': 'test123test135'
    }
    return BoonAiApp(key_dict)
