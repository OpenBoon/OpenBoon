import unittest
from unittest.mock import patch

import zmlp
import zmlp_admin

from zmlp.client import ZmlpClient


class ProjectAppTests(unittest.TestCase):

    def setUp(self):
        self.key_dict = {
            'accessKey': 'test123test135',
            'secretKey': 'test123test135'
        }
        self.app = zmlp.ZmlpApp(self.key_dict)
        self.project_app = zmlp_admin.ZmlpAdminApp(self.app).projects

    @patch.object(ZmlpClient, 'post')
    def test_create_project(self, post_patch):
        post_patch.return_value = mock_project
        project = self.project_app.create_project('cats', size=zmlp_admin.ProjectSize.LARGE,
                                                  tier=zmlp.ProjectTier.ESSENTIALS, pid="1234")
        assert_project(project)

    @patch.object(ZmlpClient, 'get')
    def test_get_project(self, get_patch):
        get_patch.return_value = mock_project
        project = self.project_app.get_project('1234')
        assert_project(project)


def assert_project(project):
    assert project.id == mock_project['id']
    assert project.name == mock_project['name']
    assert project.tier == zmlp.ProjectTier[mock_project['tier']]


mock_project = {
    'id': '12345',
    'name': 'mario',
    'tier': 'ESSENTIALS'
}
