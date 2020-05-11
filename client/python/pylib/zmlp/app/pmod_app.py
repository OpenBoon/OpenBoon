import logging

from ..entity import PipelineMod
from ..util import as_collection, as_id

logger = logging.getLogger(__name__)

__all__ = [
    'PipelineModApp'
]


class PipelineModApp:
    """
    App class for querying PipelineModules.
    """

    def __init__(self, app):
        self.app = app

    def get_pipeline_mod(self, id):
        """
        Get  PipelineMod by Id.

        Args:
            id (str): The PipelineMod ID or a PipelineMod instance.

        Returns:
            PipelineMod: The matching PipelineMod
        """
        return PipelineMod(self.app.client.get('/api/v1/pipeline-mods/{}'.format(as_id(id))))

    def find_one_pipeline_mod(self, id=None, name=None, type=None, category=None, provider=None):
        """
        Find a single PipelineMod based on various properties.

        Args:
            id (str): The ID or list of Ids.
            name (str): The model name or list of names.
            type: (str): A PipelineMod typ type or collection of types to filter on.
            category (str): The category of PipelineModule
            provider (str): The provider of the PipelineModule
        Returns:
            PipelineMod: The matching PipelineMod.
        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type),
            'categories': as_collection(category),
            'providers': as_collection(provider)
        }
        return PipelineMod(self.app.client.post('/api/v1/pipeline-mods/_find_one', body))

    def find_pipeline_mods(self, keywords=None, id=None, name=None, type=None,
                           category=None, provider=None, limit=None, sort=None):
        """
        Search for PipelineMods.

        Args:
            keywords(str): Keywords that match various fields on a PipelineMod
            id (str): An ID or collection of IDs to filter on.
            name (str): A name or collection of names to filter on.
            type: (str): A PipelineMod type type or collection of types to filter on.
            category (str): The category or collection of category names.
            provider (str): The provider or collection provider names.
            limit: (int) Limit the number of results.
            sort: (list): A sort array, example: ["time_created:desc"]

        Returns:
            generator: A generator which will return matching PipelineMods when iterated.
        """
        body = {
            'keywords': str(keywords),
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type),
            'categories': as_collection(category),
            'providers': as_collection(provider),
            'sort': sort
        }
        return self.app.client.iter_paged_results(
            '/api/v1/pipeline-mods/_search', body, limit, PipelineMod)
