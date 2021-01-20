import logging
import os
import shutil
from pwd import getpwnam

logger = logging.getLogger(__name__)


class FileCache:
    """
    A simple class for managing a cache of files for a given key
    in a task dictionary.

    """
    def __init__(self, dirname, key):
        self.root = os.path.join(os.environ.get("ANALYST_CACHE_ROOT", "/tmp"), dirname)
        self.key = key

    def create_cache_path(self, task):
        """
        Create a director for the given Task.

        Args:
            task (dict): A task dictionary.

        Returns:
            str: The cache path.
        """
        path = self.get_cache_path(task)
        os.makedirs(path, mode=0o777, exist_ok=True)
        os.chmod(path, 0o777)
        logger.info(f"creating cache {path}")
        try:
            zorroa_user = getpwnam("zorroa")
            os.chown(path, zorroa_user.pw_uid, zorroa_user.pw_gid)
        except Exception:
            pass
        return path

    def remove_cache(self, task):
        """
        Remove the cache directory.

        Args:
            task (dict): A Task dictionary.

        """
        path = self.get_cache_path(task)
        if not os.path.exists(path):
            return
        logger.info(f"removing cache {path}")
        try:
            shutil.rmtree(path)
        except Exception as e:
            logger.warning("Failed to remove cache, ", e)

    def get_cache_path(self, task):
        """
        Return the task cache path

        Args:
            task (dict): A Task dictionary.

        Returns:
            str: The cache path
        """
        return os.path.join(self.root, task[self.key])

    def clear_cache_root(self):
        """
        Clear the cache root area.
        """
        try:
            shutil.rmtree(self.root)
        except FileNotFoundError:
            pass
        except Exception as e:
            logger.warning("Failed to clear cache root: {}".format(self.root), e)


class ModelCacheManager:
    """
    The ModelCacheManager handles creating and destroying per-project
    model cache directories on the Analyst.
    """

    instance = FileCache("model-cache", "projectId")

    @classmethod
    def create_model_cache(cls, task):
        """
        Create a model cache path for the given Task.

        Args:
            task (dict): A task dictionary.

        Returns:
            str: The model cache path.
        """
        return cls.instance.create_cache_path(task)

    @classmethod
    def remove_model_cache(cls, task):
        """
        Remove the model cache.

        Args:
            task (dict): A Task dictionary.

        """
        cls.instance.remove_cache(task)

    @classmethod
    def get_model_cache_path(cls, task):
        """
        Get the model cache path.  If the ZVI_MODEL_CACHE_ROOT environment variable
        is set then that location is used for the model cache root.  This is
        usually /model-cache on an analyst.  If no env car is set, then the
        temp dir is used.  This would be the case in a unittest environment.

        Args:
            task (dict): The Task dictionary.

        Returns:
            str: The path to the model cache for a task.

        """
        return cls.instance.get_cache_path(task)


class TaskCacheManager:
    """
    TaskCacheManager handles creating and destroying the running task cache.  The Task
    cache is mounted to the container as /tmp, which serves as a place to store
    cached proxy images, etc.
    """

    instance = FileCache("task-cache", "id")

    @classmethod
    def create_task_cache(cls, task):
        """
        Creates the task cache directory.

        Args:
            task (dict): A Task dictionary.

        Returns:
            str: The task cache path.
        """
        return cls.instance.create_cache_path(task)

    @classmethod
    def remove_task_cache(cls, task):
        """
        Remove the task cache path.  This is generally done once a task completes.

        Args:
            task (dict): A Task dictionary.

        """
        cls.instance.remove_cache(task)

    @classmethod
    def get_task_cache_path(cls, task):
        """
        Return the task cache path

        Args:
            task (dict): A Task dictionary.

        Returns:
            str: The cache path
        """
        return cls.instance.get_cache_path(task)
