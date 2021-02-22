from boonsdk.util import as_id, as_id_collection, as_collection

from ..entity import Index, IndexTask

__all__ = [
    'IndexApp'
]


class IndexApp:
    """
    The IndexApp handles managing per-project ES indexes.

    """
    def __init__(self, app):
        self.app = app

    def create_index(self, size, project=None):
        """
        Create a new IndexRoute of the specified size.  The index mapping version and
        cluster are chosen automatically.  The operations happen under whatever the
        currently authed project is unless a project ID is supplied.

        Args:
            size (IndexSize): controls the number of shards and replicas.
            project (Project): An optional Project or project unique Id.
        Returns:
            IndexRoute: The new route.
        """
        body = {
            'size': size.name,
            'projectId': as_id(project)
        }
        return Index(self.app.client.post('/api/v2/index-routes', body))

    def migrate_index(self, src_index, dst_index):
        """
        Migrate all data from the src_index to the dst_index. The project is swapped to
        the index on completion. The old index is closed but not deleted right away.

        Args:
            src_index (IndexRoute): The src index or its unique Id.
            dst_index (IndexRoute): The dst index or its unique Id.

        Returns:
            IndexTask: A async task which describes the operation.

        """
        body = {
            'srcIndexRouteId': as_id(src_index),
            'dstIndexRouteId': as_id(dst_index)
        }
        return IndexTask(self.app.client.post('/api/v1/index-routes/_migrate', body))

    def find_indexes(self, index=None, project=None, cluster=None,
                     mappings=None, project_name=None, limit=None, sort=None):
        """
        Search for Indexes based on filter args.  All args can be a collection or
        a scalar value.

        Returns:
            Generator: A generator which produces Index objects
        """
        body = {
            'indexId': as_id_collection(index),
            'projectIds': as_id_collection(project),
            'projectNames':  as_id_collection(project_name),
            'clusterIds': as_id_collection(cluster),
            'mappings': as_collection(mappings),
            'sort': sort
        }
        return self.app.client.iter_paged_results('/api/v1/index-routes/_search',
                                                  body, limit, Index)

    def migrate_project_index(self, project, mapping, version, size=None, cluster=None):
        """
        Migrate the project's index to a new index with the given mapping and version.

        Args:
            project: (Project): The Project or unique project id.
            mapping (str): The name of the mapping.
            version (int): The mapping version name.
            size: (ProjectSize): Optional predicted size of the project.
                None for use existing size.
            cluster (Cluster): Optional cluster or unique cluster ID,
                defaults to projects current cluster.

        Returns:
            IndexTask: An async index task which executes the migration.
        """
        pid = as_id(project)
        if size:
            size = size.name
        body = {
            'mapping': mapping,
            'majorVer': version,
            'size': size,
            'clusterId': as_id(cluster)
        }
        return IndexTask(self.app.client.post(f'/api/v1/projects/{pid}/_migrate', body))

    def get_index_attrs(self, index):
        """
        Get the full state of the ES index.

        Args:
            index (Index): The index or the index unique Id.

        Returns:
            dict: The index state
        """
        index = as_id(index)
        return self.app.client.get(f'/api/v1/index-routes/{index}/_attrs')

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
