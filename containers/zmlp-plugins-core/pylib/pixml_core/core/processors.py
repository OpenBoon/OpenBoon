import logging

from pixml.analysis import AssetBuilder, Argument

logger = logging.getLogger(__name__)


class GroupProcessor(AssetBuilder):
    """A GroupProcessor is for holding sub processors. By itself, GroupProcessor is a no-op."""
    def __init__(self):
        super(GroupProcessor, self).__init__()

    def process(self, frame):
        pass


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
