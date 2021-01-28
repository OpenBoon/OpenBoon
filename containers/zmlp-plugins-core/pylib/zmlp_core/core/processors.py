import os
from zlib import adler32

from zmlpsdk import AssetProcessor, Argument, ZmlpFatalProcessorException
from zmlpsdk.storage import file_storage


class GroupProcessor(AssetProcessor):
    """A GroupProcessor is for holding sub processors. By itself, GroupProcessor is a no-op."""

    def __init__(self):
        super(GroupProcessor, self).__init__()

    def process(self, frame):
        pass


class PreCacheSourceFileProcessor(AssetProcessor):
    """PreCacheSourceFileProcessor pre-caches the source path and adds some additional
    data to the source namespace
    """

    def process(self, frame):
        asset = frame.asset
        try:

            if asset.get_attr('system.state') == 'Analyzed':
                self.logger.info('Not precaching source, file is already analyzed')
                return

            self.logger.info('precaching source file')
            path = file_storage.localize_file(asset)
            if not asset.get_attr('source.filesize'):
                asset.set_attr('source.filesize', os.path.getsize(path))
            if not asset.get_attr('source.checksum'):
                asset.set_attr('source.checksum', self.calculate_checksum(path))

        except Exception as e:
            self.logger.exception('Failed to pre-cache source file')
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


class AssertAttributesProcessor(AssetProcessor):
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


class SetAttributesProcessor(AssetProcessor):
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
            self.logger.debug('setting attrs: %s' % self.arg_value('attrs'))
            for k, v in self.arg_value('attrs').items():
                frame.asset.set_attr(k, v)

        if self.arg_value('remove_attrs'):
            self.logger.debug('removing attrs: %s' % self.arg_value('remove_attrs'))
            for k in self.arg_value('remove_attrs'):
                frame.asset.del_attr(k)


class DeleteBySearchProcessor(AssetProcessor):

    file_types = None

    def __init__(self):
        super(DeleteBySearchProcessor, self).__init__()
        self.add_arg(Argument('dataSourceId', 'str', default={}))

    def process(self, frame):
        ds_id = self.arg_value('dataSourceId')
        self.assets_batch_delete(ds_id)

    def assets_batch_delete(self, data_source_id):
        batch_size = 100
        query = {
            '_source': False,
            'size': 20,
            'query': {
                'term': {
                    'system.dataSourceId': data_source_id
                }
            }
        }

        self.logger.info('Querying and Deleting assets. DataSource id: {}'.format(data_source_id))
        batch = []

        for a in self.app.assets.scroll_search(query):
            batch.append(a.id)
            if len(batch) >= batch_size:
                self.logger.info(self.app.assets.batch_delete_assets(batch))
                batch = []

        # Handle left over batch
        if batch:
            self.logger.info(self.app.assets.batch_delete_assets(batch))
