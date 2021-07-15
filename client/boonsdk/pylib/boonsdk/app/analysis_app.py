import logging

from ..entity import AnalysisModule
from ..util import as_collection, as_id, is_valid_uuid
from ..search import PredictionLabelsAggregation

logger = logging.getLogger(__name__)

__all__ = [
    'AnalysisModuleApp'
]


class AnalysisModuleApp:
    """
    App class for querying Analysis Modules
    """

    def __init__(self, app):
        self.app = app

    def get_analysis_module(self, id):
        """
        Get an AnalysisModule by Id.

        Args:
            id (str): The AnalysisModule ID or a AnalysisModule instance.

        Returns:
            AnalysisModule: The matching AnalysisModule
        """
        if is_valid_uuid(as_id(id)):
            return AnalysisModule(self.app.client.get('/api/v1/pipeline-mods/{}'.format(as_id(id))))
        else:
            return self.find_one_analysis_module(name=id)
        return

    def find_one_analysis_module(self, id=None, name=None, type=None, category=None, provider=None):
        """
        Find a single AnalysisModule based on various properties.

        Args:
            id (str): The ID or list of Ids.
            name (str): The model name or list of names.
            type: (str): A AnalysisModule typ type or collection of types to filter on.
            category (str): The category of AnalysisModuleule
            provider (str): The provider of the AnalysisModuleule
        Returns:
            AnalysisModule: The matching AnalysisModule.
        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type),
            'categories': as_collection(category),
            'providers': as_collection(provider)
        }
        return AnalysisModule(self.app.client.post('/api/v1/pipeline-mods/_find_one', body))

    def find_analysis_modules(self, keywords=None, id=None, name=None, type=None,
                              category=None, provider=None, limit=None, sort=None):
        """
        Search for AnalysisModule.

        Args:
            keywords(str): Keywords that match various fields on a AnalysisModule
            id (str): An ID or collection of IDs to filter on.
            name (str): A name or collection of names to filter on.
            type: (str): A AnalysisModule type type or collection of types to filter on.
            category (str): The category or collection of category names.
            provider (str): The provider or collection provider names.
            limit: (int) Limit the number of results.
            sort: (list): A sort array, example: ["time_created:desc"]

        Returns:
            generator: A generator which will return matching AnalysisModules when iterated.
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
            '/api/v1/pipeline-mods/_search', body, limit, AnalysisModule)

    def get_prediction_counts(self, module, min_score=0.0, max_score=1.0):
        """
        Return a dictionary of predicted label : counts.

        Args:
            module (AnalysisModule): The AnalysisModule, it's name or unqiue ID.
            min_score (float): The minimum score for counted predictions.
            max_score (float): The maximum score for counted predictions.

        Returns:
            dict: A dict of predicted_label : count.

        """
        mod = self.get_analysis_module(module)
        agg = PredictionLabelsAggregation("preds", mod.name,
                                          min_score=min_score, max_score=max_score)
        search = {
            "count": 0,
            "aggs": agg
        }
        result = self.app.assets.search(search)
        raw = result.aggregation(agg)
        if not raw:
            return {}
        return dict(((item['key'], item['doc_count']) for item in raw['buckets']))
