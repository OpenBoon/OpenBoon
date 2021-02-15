from .base import BaseEntity

__all__ = [
    'AnalysisModule'
]


class AnalysisModule(BaseEntity):
    """
    An AnalysisModule describes a type of ML process that gets applied to an asset.

    """
    def __init__(self, data):
        super(AnalysisModule, self).__init__(data)

    @property
    def name(self):
        """The name of the AnalysisModule"""
        return self._data['name']

    @property
    def type(self):
        """The type of ML operation the AnalysisModule accomplishes."""
        return self._data['type']

    @property
    def supported_media(self):
        """The types of media supported by the AnalysisModule """
        return self._data['supportedMedia']

    @property
    def category(self):
        """The category/brand of AnalysisModule, example: Google Video Intelligence"""
        return self._data['category']

    @property
    def provider(self):
        """The provider of the AnalysisModule, example as Zorroa, Google, Amazon"""
        return self._data['provider']

    @property
    def description(self):
        """The description of the AnalysisModule"""
        return self._data['description']
