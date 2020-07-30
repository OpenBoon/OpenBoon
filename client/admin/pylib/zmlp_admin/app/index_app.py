from zmlp.util import as_id

from ..entity import Index, IndexTask

__all__ = [
    'IndexApp'
]


class IndexApp:

    def __init__(self, app):
        self.app = app

    def create_index(self, shards, replicas, project=None, mapping='english_strict', version=1):
        """
        Create a new IndexRoute, which routes asset metadata to an ES index.  Creating a new route
        doesn't affect the project in any way.

        For test projects with low numbers of assets, 1 shard, 0 replicas is fine.  For xlarge
        projects (1 million+), 5-7 shards and 1-2 replicas.

        Args:
            shards (int): The number of shards.
            replicas (int): The number of replicas.
            project (Project): The project or unique project id, defaults to authed project.
            mapping (str): The name of the mapping.
            version (int): The version of the mapping to create.

        Returns:
            IndexRoute: The new route.
        """
        body = {
            'mapping': mapping,
            'majorVer': int(version),
            'shards': shards,
            'replicas': replicas,
            'projectId': as_id(project)
        }
        return Index(self.app.client.post('/api/v1/index-routes', body))

    def migrate_index(self, src_index, dst_index):
        """
        Migrate the data in the src_index to the dst_index.

        Args:
            src_index (IndexRoute): The src index route or its unique Id.
            dst_index (IndexRoute); The dst index route or its unique Id.

        Returns:
            IndexTask: A async task which describes the operation.

        """
        body = {
            'srcIndexRouteId': as_id(src_index),
            'dstIndexRouteId': as_id(dst_index)
        }
        return IndexTask(self.app.client.post('/api/v1/index-routes/_migrate', body))

    def get_index_task(self, tid):
        """
        Get an IndexTask by unique Id.  IndexTasks are managed by ElasticSearch and are
        not ZVI job tasks.

        Args:
            tid (str): The task id.

        Returns:
            IndexTask: The index task.
        """
        tid = as_id(tid)
        return IndexTask(self.app.client.get(f'/api/v1/index-tasks/{tid}'))

    def get_index_tasks(self):
        """
        Return a list of active index tasks.

        Returns:
            list: The list of tasks.
        """

        return [IndexTask(task) for task in self.app.client.get('/api/v1/index-tasks')]

    def get_es_task_info(self, tid):
        """
        Get the associated ES task metadata for a given task.

        Args:
            tid (str): The task id.

        Returns:
            dict: Metadata concerning the associated ES task.
        """
        tid = as_id(tid)
        return self.app.client.get(f'/api/v1/index-tasks/{tid}/_es_task_info')
