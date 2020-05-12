from .base import BaseEntity

__all__ = [
    'PipelineMod'
]


class PipelineMod(BaseEntity):
    """
    A PipelineModule is used in conjunction with DataSources or Pipelines to
    define the processes by which data is ingested.

    """
    def __init__(self, data):
        super(PipelineMod, self).__init__(data)

    @property
    def name(self):
        """The name of the PipelineMod"""
        return self._data['name']

    @property
    def type(self):
        """The type of ML operation the PipelineMod accomplishes."""
        return self._data['type']

    @property
    def supported_media(self):
        """The types of media supported by the PipelineMod """
        return self._data['supportedMedia']

    @property
    def category(self):
        """The category/brand of PipelineMod, example: Google Video Intelligence"""
        return self._data['category']

    @property
    def provider(self):
        """The provider of the PipelineMod, example as Zorroa, Google, Amazon"""
        return self._data['provider']

    @property
    def description(self):
        """The description of the PipelineMod"""
        return self._data['description']

    @property
    def ops(self):
        """The discrete operations defined by the PipelineModule"""
        return self._data['ops']
