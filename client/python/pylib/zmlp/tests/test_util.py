import unittest

import zmlp.util as util
from zmlp import Project


class UtilTests(unittest.TestCase):

    def test_is_valid_uuid(self):
        yes = 'D29556D6-8CF7-411B-8EB0-60B573098C26'
        no = 'dog'

        assert util.is_valid_uuid(yes)
        assert not util.is_valid_uuid(no)

    def test_as_collection(self):
        assert ['foo'] == util.as_collection('foo')
        assert ['foo'] == util.as_collection(['foo'])

    def test_as_id(self):
        project = Project({'id': '12345'})
        assert '12345' == util.as_id(project)

    def test_as_id_collection(self):
        project = Project({'id': '12345'})
        project_id = "56781"
        assert ['12345', '56781'] == util.as_id_collection([project, project_id])
