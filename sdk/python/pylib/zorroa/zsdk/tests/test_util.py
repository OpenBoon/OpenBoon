import json
import re
import unittest
import os
from datetime import datetime

from zorroa.client import ZorroaJsonEncoder
from zorroa.zsdk import Document
from zorroa.zsdk.util.std import import_class, file_exists, str_time_now


class Tests(unittest.TestCase):

    def test_str_time_now(self):
        ts = str_time_now()
        _date, _time, offset = ts.split(" ")
        self.assertTrue(re.match(r"^\d{4}-\d{2}-\d{2}$", _date))
        self.assertTrue(re.match(r"^\d{2}:\d{2}:\d{2}$", _time))
        self.assertTrue(re.match(r"^[-+]?\d{4}$", offset))

    def test_import_class(self):
        klass = import_class('datetime.datetime')
        assert klass.today().year == datetime.today().year

    def test_file_exists(self):
        assert file_exists(__file__)
        assert file_exists(os.path.dirname(__file__))
        assert not file_exists("/spock/kirk")


class ZpsJsonEncoderUnitTest(unittest.TestCase):

    def test_serialize_document(self):
        document = Document({'id': 1})
        string = json.dumps(document, cls=ZorroaJsonEncoder)
        assert '"replace": false' in string
        assert '"document": {}' in string
        assert '"id": 1' in string

