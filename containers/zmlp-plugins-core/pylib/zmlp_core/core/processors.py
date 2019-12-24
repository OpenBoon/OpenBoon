import logging
import os
from zlib import adler32

from zmlp.analysis import AssetBuilder, Argument, ZmlpFatalProcessorException
from zmlp.analysis.storage import file_cache

logger = logging.getLogger(__name__)


class GroupProcessor(AssetBuilder):
    """A GroupProcessor is for holding sub processors. By itself, GroupProcessor is a no-op."""

    def __init__(self):
        super(GroupProcessor, self).__init__()

    def process(self, frame):
        pass


class PreCacheSourceFileProcessor(AssetBuilder):
    """PreCacheSourceFileProcessor pre-caches the source path and adds some additional
    data to the source namespace
    """

    def process(self, frame):
        asset = frame.asset
        try:
            logger.info('precaching Asset: {}'.format(asset))
            path = file_cache.localize_remote_file(asset)
            # Virtual clip assets don't get a file size or checksum.
            if not asset.attr_exists('source.filesize') and \
                    not asset.attr_exists('clip.sourceAssetId'):
                asset.set_attr('source.filesize', os.path.getsize(path))
                asset.set_attr('source.checksum', self.calculate_checksum(path))

        except Exception as e:
            logger.exception('Failed to pre-cache source file')
            raise ZmlpFatalProcessorException('Failed to pre-cache source file', e)

    def calculate_checksum(self, path):
        checksum = 0
        # This looks wonky but it calculates the
        # same checksum reading in all the bytes
        with open(path, 'rb') as fp:
            chunk = fp.read(8192)
            if chunk:
                checksum = adler32(chunk)
                while True:
                    chunk = fp.read(8192)
                    if chunk:
                        checksum = adler32(chunk, checksum)
                    else:
                        break
        return checksum


class AssertAttributesProcessor(AssetBuilder):
    """AssertAttributesProcessor checks for the existence of a list of attributes.

    Args:
        attrs (list): List of attributes, keys should be in dot notation.

    """
    toolTips = {
        'attrs': 'List of attributes, keys should be in dot notation.'
    }

    def __init__(self):
        super(AssertAttributesProcessor, self).__init__()
        self.add_arg(Argument('attrs', 'list', default=None, toolTip=self.toolTips['attrs']))

    def process(self, frame):
        asset = frame.asset
        attrs = self.arg_value("attrs")
        if not attrs:
            return
        for attr in attrs:
            if not asset.attr_exists(attr):
                raise ZmlpFatalProcessorException(
                    "The '{}' attr was missing from the asset '{}'".format(attr, asset.uri))


class SetAttributesProcessor(AssetBuilder):
    """SetAttributesProcessor accepts a map of attributes and sets them on the document.

    Args:
        attrs (dict): Dict of attributes, keys should be in dot notation.

    """
    toolTips = {
        'attrs': 'Dict of attributes, keys should be in dot notation.',
        'remove_attrs': 'List of attributes to remove.'
    }

    def __init__(self):
        super(SetAttributesProcessor, self).__init__()
        self.add_arg(Argument('attrs', 'dict', default={}, toolTip=self.toolTips['attrs']))
        self.add_arg(Argument('remove_attrs', 'list',
                              default={}, toolTip=self.toolTips['remove_attrs']))

    def process(self, frame):

        if self.arg_value('attrs'):
            logger.debug('setting attrs: %s' % self.arg_value('attrs'))
            for k, v in self.arg_value('attrs').items():
                frame.asset.set_attr(k, v)

        if self.arg_value('remove_attrs'):
            logger.debug('removing attrs: %s' % self.arg_value('remove_attrs'))
            for k in self.arg_value('remove_attrs'):
                frame.asset.del_attr(k)
