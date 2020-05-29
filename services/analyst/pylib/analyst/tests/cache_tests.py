import logging
import os
import unittest

from analyst.cache import ModelCacheManager, TaskCacheManager

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

task = {
    "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
    "jobId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
    "projectId": "81C54046-6452-4669-BD71-719E9D5C2BBF",
    "name": "process_me",
}


class TestModelCacheManager(unittest.TestCase):

    def test_create_model_cache(self):
        path = ModelCacheManager.create_model_cache(task)
        assert os.path.exists(path)
        assert path.endswith(task['projectId'])

    def test_get_model_cache(self):
        path = ModelCacheManager.get_model_cache_path(task)
        assert path.endswith(task['projectId'])

    def test_remove_model_cache(self):
        path = ModelCacheManager.create_model_cache(task)
        assert os.path.exists(path)
        ModelCacheManager.remove_model_cache(task)
        assert not os.path.exists(path)


class TestTaskCacheManager(unittest.TestCase):

    def test_create_task_cache(self):
        path = TaskCacheManager.create_task_cache(task)
        assert os.path.exists(path)
        assert path.endswith(task['id'])

    def test_get_task_cache(self):
        path = TaskCacheManager.get_task_cache_path(task)
        assert path.endswith(task['id'])

    def test_remove_task_cache(self):
        path = TaskCacheManager.create_task_cache(task)
        assert os.path.exists(path)
        TaskCacheManager.remove_task_cache(task)
        assert not os.path.exists(path)
