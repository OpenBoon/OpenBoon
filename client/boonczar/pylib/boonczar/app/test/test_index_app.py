import unittest
from unittest.mock import patch

import boonsdk
import boonczar
from boonsdk.client import BoonClient


class IndexAppTests(unittest.TestCase):

    def setUp(self):
        self.key_dict = {
            'accessKey': 'test123test135',
            'secretKey': 'test123test135'
        }
        self.app = boonsdk.BoonApp(self.key_dict)
        self.index_app = boonczar.BoonCzarApp(self.app).indexes

    @patch.object(BoonClient, 'post')
    def test_create_index(self, post_patch):
        post_patch.return_value = mock_index_data
        index = self.index_app.create_index(boonczar.IndexSize.LARGE)
        assert_mock_index(index)

    @patch.object(BoonClient, 'post')
    def test_migrate_index(self, post_patch):
        post_patch.return_value = mock_index_task_data
        task = self.index_app.migrate_index('abc123', '456789')
        assert_mock_task(task)

    @patch.object(BoonClient, 'get')
    def test_get_index_task(self, get_patch):
        get_patch.return_value = mock_index_task_data
        task = self.index_app.get_index_task('abc123')
        assert_mock_task(task)

    @patch.object(BoonClient, 'get')
    def test_get_index_tasks(self, get_patch):
        get_patch.return_value = [mock_index_task_data]
        tasks = self.index_app.get_index_tasks()
        assert_mock_task(tasks[0])

    @patch.object(BoonClient, 'get')
    def test_get_es_task_info(self, get_patch):
        get_patch.return_value = mock_es_task_info
        info = self.index_app.get_es_task_info('abc123')
        assert info['action'] == 'indices:data/write/reindex'

    @patch.object(BoonClient, 'post')
    def test_migrate_project_index(self, post_patch):
        post_patch.return_value = mock_index_task_data
        task = self.index_app.migrate_project_index('1234', 'english_strict', 2)
        assert_mock_task(task)

    @patch.object(BoonClient, 'post')
    def test_get_indexes(self, post_patch):
        post_patch.return_value = {'list': [mock_index_data]}
        for index in self.index_app.find_indexes(limit=1):
            assert_mock_index(index)

    @patch.object(BoonClient, 'get')
    def test_get_index_attrs(self, get_patch):
        get_patch.return_value = {'name': 'foo'}
        index = self.index_app.get_index_attrs('foo')
        assert 'foo' == index['name']


def assert_mock_index(index):
    assert index.id == mock_index_data['id']
    assert index.project_id == mock_index_data['projectId']
    assert index.cluster_id == mock_index_data['clusterId']
    assert index.cluster_url == mock_index_data['clusterUrl']
    assert index.state == boonczar.entity.IndexState[mock_index_data['state']]
    assert index.index_name == mock_index_data['indexName']
    assert index.shards == mock_index_data['shards']
    assert index.replicas == mock_index_data['replicas']
    assert index.mapping_version == 'english_strict-1.2'


def assert_mock_task(task):
    assert task.id == mock_index_task_data['id']
    assert task.project_id == mock_index_task_data['projectId']
    assert task.src_index_id == mock_index_task_data['srcIndexRouteId']
    assert task.dst_index_id == mock_index_task_data['dstIndexRouteId']
    assert task.name == mock_index_task_data['name']
    assert task.type == boonczar.IndexTaskType[mock_index_task_data['type']]
    assert task.state == boonczar.IndexTaskState[mock_index_task_data['state']]
    assert task.es_task_id == mock_index_task_data['esTaskId']


mock_index_data = {
    'id': '12345',
    'projectId': 'c9a89e38-ca36-441a-a9bf-02e6c3f8f55e',
    'clusterId': 'c9a89e38-ca36-441a-a9bf-02e6c3f8f551',
    'clusterUrl': 'http://foo.bar',
    'state': 'READY',
    'indexName': 'kdfjksajkjkdf',
    'mapping': 'english_strict',
    'majorVer': 1,
    'minorVer': 2,
    'replicas': 1,
    'shards': 2,
    'indexUrl': 'http://foo.bar/kdfjksajkjkdf'
}

mock_index_task_data = {
    'id': '12345',
    'projectId': 'c9a89e38-ca36-441a-a9bf-02e6c3f8f55e',
    'srcIndexRouteId': 'c9a89e38-ca36-441a-a9bf-02e6c3f8f551',
    'dstIndexRouteId': 'a1a89e38-ca36-441a-a9bf-02e6c3f8f551',
    'name': 'woowoo',
    'type': 'REINDEX',
    'state': 'RUNNING',
    'esTaskId': 'abc123:12345'
}

mock_es_task_info = {
    'action': 'indices:data/write/reindex'
}
