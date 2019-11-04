from datetime import date

from zplugins.google.collectors import PubSubCollector
from zsdk import Frame, Asset
from zsdk.testing import PluginUnitTestCase


class PubSubCollectorUnitTestCase(PluginUnitTestCase):
    def test_get_data(self):
        asset = Asset()
        asset.set_attr('set', {1, 2})
        asset.set_attr('date', date(2000, 1, 1))
        frames = [Frame(asset)]
        collector = PubSubCollector()
        data = collector._get_data(frames)
        assert data == '[{"links": null, "replace": false, ' \
                       '"parentId": null, "document": {"date": "2000-01-01", ' \
                       '"set": [1, 2]}, "type": "asset", "id": null, "permissions": null}]'
