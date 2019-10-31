import urllib
import logging

from pathlib2 import Path

from zsdk.processor import DocumentProcessor, Argument
from zsdk.exception import UnrecoverableProcessorException

from archivist.asset import get_asset
from archivist.client import _client

logger = logging.getLogger(__name__)


class GroupProcessor(DocumentProcessor):
    """A GroupProcessor is for holding sub processors. By itself, GroupProcessor is a no-op."""
    def __init__(self):
        super(GroupProcessor, self).__init__()

    def _process(self, frame):
        pass


class SetAttributesProcessor(DocumentProcessor):
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

    def _process(self, frame):

        if self.arg_value('attrs'):
            logger.debug('setting attrs: %s' % self.arg_value('attrs'))
            for k, v in self.arg_value('attrs').items():
                frame.asset.set_attr(k, v)

        if self.arg_value('remove_attrs'):
            logger.debug('removing attrs: %s' % self.arg_value('remove_attrs'))
            for k in self.arg_value('remove_attrs'):
                frame.asset.del_attr(k)


class PythonScriptProcessor(DocumentProcessor):
    """Execute a Python script.

    Args:
        script (str): The text of a python script to be executed. _frame and _doc are exposed.

    """
    toolTips = {
        'script': 'The text of a python script to be executed. _frame and _doc are exposed.'
    }

    def __init__(self):
        super(PythonScriptProcessor, self).__init__()
        self.add_arg(Argument('script', 'string', default='', toolTip=self.toolTips['script']))

    def _process(self, frame):
        exec(self.arg_value('script'), {'_self': self,
                                        '_frame': frame,
                                        '_doc': frame.asset})


class AssertProcessor(DocumentProcessor):
    """AssertProcessor evaluates a Python expression and emits an error if
    the expression evaluates to true.

    Args:
        script (str): Python expression to evaluate, True means error.
        message (str): Message to send if the expression is True.
        fatal: (bool): If True then the frame should be skipped on a failed assert.

    """
    toolTips = {
        'script': 'Python expression to evaluate, True means error.',
        'message': 'Message to send if the expression is True.',
        'fatal': 'If True then the frame should be skipped on a failed assert.'
    }

    def __init__(self):
        super(AssertProcessor, self).__init__()
        self.add_arg(Argument('script', 'string', required=True, toolTip=self.toolTips['script']))
        self.add_arg(Argument('message', 'string', default='No error message was set',
                              toolTip=self.toolTips['message']))
        self.add_arg(Argument('fatal', 'boolean', default=False, toolTip=self.toolTips['fatal']))

    def _process(self, frame):
        self.logger.info('Evaluating %s' % self.arg_value('script'))
        if not self.arg_value('script'):
            self.logger.warn('Skipping AssertProcessor, script is empty')
            return

        result = eval(self.arg_value('script'), {'_self': self,
                                                 '_frame': frame,
                                                 '_doc': frame.asset})
        if result:
            self.reactor.error(frame,
                               self.__class__.__name__,
                               self.arg_value('message'),
                               self.arg_value('fatal'),
                               'process')
            if self.arg_value('fatal'):
                frame.skip = True


class ReturnResponseProcessor(DocumentProcessor):
    """ReturnResponse gathers up all assets and sends them back to
    the archivist which forwards them onto waiting API calls.
    """
    def __init__(self):
        super(ReturnResponseProcessor, self).__init__()
        self.response = []

    def _process(self, frame):
        self.response.append(frame.asset)

    def teardown(self):
        self.reactor.response(self.response)


class DownloadAssetProcessor(DocumentProcessor):
    """DownloadAsset downloads a file and stores its location in the temp metadata.

    Args:
        url_attr (str): Attribute that contains the URL to download.
        destination_attr (str): Attribute to store the final location of the file.
        destination_directory (str): Directory to download the file into.

    """
    toolTips = {
        'url_attr': 'Attribute that contains the URL to download.',
        'destination_attr': 'Attribute to store the final location of the file.',
        'destination_directory': 'Directory to download the file into.'
    }

    def __init__(self):
        super(DownloadAssetProcessor, self).__init__()
        self.add_arg(Argument('url_attr', 'string', default='tmp.download_url',
                              toolTip=self.toolTips['url_attr']))
        self.add_arg(Argument('destination_attr', 'string', default='tmp.download_path',
                              toolTip=self.toolTips['destination_attr']))
        self.add_arg(Argument('destination_directory', 'string', default='/tmp',
                              toolTip=self.toolTips['destination_directory']))

    def _process(self, frame):
        asset = frame.asset
        url = asset.get_attr(self.arg_value('url_attr'))
        filename = Path(url).name
        destination_path = Path(self.arg_value('destination_directory')).joinpath(filename)
        urllib.urlretrieve(asset.get_attr(self.arg_value('url_attr')),
                           filename=str(destination_path))
        asset.set_attr(self.arg_value('destination_attr'), str(destination_path))
        self.logger.info('Downloaded file to %s.' % destination_path)


class DeleteAssetProcessor(DocumentProcessor):
    """Deletes an asset and skips all further processing of the document."""
    def _process(self, frame):
        asset = get_asset(frame.asset.id)
        asset.delete()
        frame.skip = True


class SetIdProcessor(DocumentProcessor):
    """Sets a new ID for an asset based on an existing attribute.

    Args:
        attribute (str): Unique attribute to generate a new ID from.

    """
    toolTips = {
        'attribute': 'Unique attribute to generate a new ID from.'
    }

    def __init__(self):
        super(SetIdProcessor, self).__init__()
        self.add_arg(Argument('attribute', 'string', default='source.path',
                              toolTip=self.toolTips['attribute']))

    def _process(self, frame):
        asset = frame.asset
        asset.id = asset.generate_id(attr=self.arg_value('attribute'))


class AddPermissionProcessor(DocumentProcessor):
    """Add a permission to the current asset.

    Args:
        name (str): Name.
        access (int): The access level.
        create (bool): Create the permission if it doesn't already exist.

    """
    toolTips = {
        'name': 'Name.',
        'access': 'The access level.',
        'create': 'Create the permission if it doesn\'t already exist.'
    }

    def __init__(self):
        super(AddPermissionProcessor, self).__init__()
        self.add_arg(Argument('name', 'str', required=True, toolTip=self.toolTips['name']))
        self.add_arg(Argument('access', 'int', required=True, default=1,
                              toolTip=self.toolTips['access']))
        self.add_arg(Argument('create', 'boolean', default=False,
                              required=True, toolTip=self.toolTips['create']))
        self.checked_names = set()

    def _process(self, frame):
        access = self.arg_value('access')
        name = self.arg_value('name')
        parts = name.split('::')
        if len(parts) != 2:
            raise UnrecoverableProcessorException('Invalid permission name: ' + name)

        if self.arg_value('create') and name not in self.checked_names:
            if not self.permission_exists(name):
                _client.post('/api/v1/permissions', {
                    'type': parts[0],
                    'name': parts[1],
                    'description': name + ' created by batch job',
                    'source': 'Permission Processor'
                })

        self.checked_names.add(name)

        if frame.asset.permissions is None:
            frame.asset.permissions = {}

        frame.asset.permissions[name] = access

    def permission_exists(self, name):
        """
        Return True if a permission with the given name exists.

        Args:
            name (str): The full permission name, ex zorroa::read

        Returns:
            bool: True if permission exists, false if not.

        """
        return _client.get('/api/v2/permissions/_exists', {"fullName": name})
