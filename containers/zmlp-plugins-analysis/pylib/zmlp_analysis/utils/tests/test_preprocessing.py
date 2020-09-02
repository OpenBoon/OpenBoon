import logging
import pytest

from zmlp_analysis.utils.preprocessing import flatten_content, remove_parentheticals
from zmlpsdk import ZmlpFatalProcessorException
from zmlpsdk.testing import PluginUnitTestCase

logging.basicConfig()


class TestPreprocessing(PluginUnitTestCase):

    def test_flatten_content(self):
        test_list = ['this', 'is', 'a', 'test']
        result = flatten_content(test_list)
        assert result == 'this is a test'

        test_str = 'this is a test'
        result = flatten_content(test_str)
        assert result == 'this is a test'

        with pytest.raises(ZmlpFatalProcessorException):
            flatten_content(1)

    def test_remove_parentheticals(self):
        test_str = '[this is a test] not between brackets [new string test]'
        result = remove_parentheticals(test_str)

        assert result == ' not between brackets '
