from zmlp.util import as_id

from ..entity import Index, IndexTask

__all__ = [
    'IndexApp'
]


class IndexApp:

    def __init__(self, app):
        self.app = app

    def create_index(self, size):
        """
        Create a new IndexRoute of the specified size.  The index mapping version and
        cluster are chosen automatically.  The index will be created under
        the currently authed project.

        Args:
            size (ProjectSize): controls the number of shards and replicas.

        Returns:
            IndexRoute: The new route.
        """
        body = {
            'size': size.name
        }
        return Index(self.app.client.post('/api/v2/index-routes', body))

    def migrate_index(self, dst_index, src_index=None):
        """
        Migrate all data for the currently authed project into the dst_index.
        The project is swapped to the index on completion. The old index is
        closed but not deleted right away.

        If a src_index is provided, then the data is migrated from that index instead
        of the currently authed index.

        Args:
            dst_index (IndexRoute): The dst route or its unique Id.
            src_index (IndexRoute): The src route. Defaults to none which uses the
                current project's active index.

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
