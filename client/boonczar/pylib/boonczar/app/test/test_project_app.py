import unittest
from unittest.mock import patch

import boonsdk
import boonczar

from boonsdk.client import BoonSdkClient


class ProjectAppTests(unittest.TestCase):

    def setUp(self):
        self.key_dict = {
            'accessKey': 'test123test135',
            'secretKey': 'test123test135'
        }
        self.app = boonsdk.BoonSdkApp(self.key_dict)
        self.project_app = boonczar.BoonCzarApp(self.app).projects

    @patch.object(BoonSdkClient, 'post')
    def test_create_project(self, post_patch):
        post_patch.return_value = mock_project
        project = self.project_app.create_project('cats', size=boonczar.IndexSize.LARGE,
                                                  tier=boonsdk.ProjectTier.ESSENTIALS, pid="1234")
        assert_project(project)

    @patch.object(BoonSdkClient, 'get')
    def test_get_project(self, get_patch):
        get_patch.return_value = mock_project
        project = self.project_app.get_project('1234')
        assert_project(project)


def assert_project(project):
    assert project.id == mock_project['id']
    assert project.name == mock_project['name']
    assert project.tier == boonsdk.ProjectTier[mock_project['tier']]


mock_project = {
    'id': '12345',
    'name': 'mario',
    'tier': 'ESSENTIALS'
}

mock_index = {

}
