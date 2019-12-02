import unittest

from pixml.util import is_valid_uuid


class UtilTests(unittest.TestCase):

    def test_is_valid_uuid(self):
        yes = "D29556D6-8CF7-411B-8EB0-60B573098C26"
        no = "dog"

        assert is_valid_uuid(yes)
        assert not is_valid_uuid(no)

