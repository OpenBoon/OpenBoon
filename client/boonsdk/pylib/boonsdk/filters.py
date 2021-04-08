"""Special filters used for Asset Search."""
from .util import as_id, as_collection


__all__ = [
    'apply_search_filters',
    'ExcludeTrainingSetsFilter',
    'TrainingSetFilter'
]


def apply_search_filters(search, filters):
    """
    Apply the given filters to an Asset search dict. The dict is modified in place.

    Args:
        search:
        filters:
    """
    if not filters:
        return search

    for filt in as_collection(filters):
        if getattr(filt, 'asset_search_filter', None):
            filt = filt.asset_search_filter()
        search.update(filt.for_json())


class ExcludeTrainingSetsFilter:
    """
    A Asset search filter which automatically excludes all training sets.
    """
    def for_json(self):
        return {'exclude_training_sets': True}


class TrainingSetFilter:
    """
    A Asset search filter which filters Assets.
    """
    def __init__(self, model, scopes=None, labels=None):
        self.model = model
        self.scopes = [s.name for s in as_collection(scopes)] if scopes else None
        self.labels = as_collection(labels) if labels else None

    def for_json(self):
        return {
            'training_set': {
                'modelId': as_id(self.model),
                'scopes': self.scopes,
                'labels': self.labels
            }
        }
