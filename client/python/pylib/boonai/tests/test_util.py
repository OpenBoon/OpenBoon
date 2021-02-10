import unittest
import os
import zipfile

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


class ZipDirectoryTexts(unittest.TestCase):

    def test_zip_directory_no_base(self):
        output_zip = '/tmp/test-zip1.zip'
        cur_dir = os.path.dirname(__file__)
        util.zip_directory(cur_dir, output_zip)

        with zipfile.ZipFile(output_zip) as zip:
            assert 'test_search.py' in zip.namelist()
            assert 'test_util.py' in zip.namelist()

    def test_zip_directory_with_base(self):
        output_zip = '/tmp/test-zip1.zip'
        cur_dir = os.path.dirname(__file__)
        util.zip_directory(cur_dir, output_zip, 'base')

        with zipfile.ZipFile(output_zip) as zip:
            assert 'base/test_search.py' in zip.namelist()
            assert 'base/test_util.py' in zip.namelist()
