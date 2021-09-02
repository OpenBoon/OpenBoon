import copy
import os
import tempfile
import unittest
import unittest.mock as mock
from unittest.mock import patch

import pytest

import boonsdk
from boonsdk import Asset, BoonClient, app_from_env, \
    FileImport, FileUpload, StoredFile, BoonSdkException, Dataset, TrainingSetFilter
from .util import get_test_file


class AssetAppTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

        self.mock_import_result = {
            'bulkResponse': {
                'took': 15,
                'errors': False,
                'items': [{
                    'create': {
                        '_index': 'yvqg1901zmu5bw9q',
                        '_type': '_doc',
                        '_id': 'dd0KZtqyec48n1q1fniqVMV5yllhRRGx',
                        '_version': 1,
                        'result': 'created',
                        'forced_refresh': True,
                        '_shards': {
                            'total': 1,
                            'successful': 1,
                            'failed': 0
                        },
                        '_seq_no': 0,
                        '_primary_term': 1,
                        'status': 201
                    }
                }]
            },
            'failed': [],
            'created': ['dd0KZtqyec48n1q1fniqVMV5yllhRRGx'],
            'jobId': 'ba310246-1f87-1ece-b67c-be3f79a80d11'
        }

        # A mock search result used for asset search tests
        self.mock_search_result = {
            'took': 4,
            'timed_out': False,
            'hits': {
                'total': {'value': 2},
                'max_score': 0.2876821,
                'hits': [
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_score': 0.2876821,
                        '_source': {
                            'source': {
                                'path': 'https://i.imgur.com/SSN26nN.jpg'
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg',
                        '_score': 0.2876821,
                        '_source': {
                            'source': {
                                'path': 'https://i.imgur.com/foo.jpg'
                            }
                        }
                    }
                ]
            }
        }

        self.mock_clip_search_result = {
            'took': 4,
            'timed_out': False,
            'hits': {
                'total': {'value': 2},
                'max_score': 0.2876821,
                'hits': [
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_score': 0.2876821,
                        '_source': {
                            'clip': {
                                'assetId': '12345',
                                'timeline': 'foo',
                                'track': 'bar',
                                'start': 0.5,
                                'stop': 1.5,
                                'content': ["everybody dance now"]
                            }
                        }
                    }
                ]
            }
        }

    @patch.object(BoonClient, 'post')
    def test_import_files(self, post_patch):
        post_patch.return_value = self.mock_import_result
        assets = [FileImport('gs://zorroa-dev-data/image/pluto.png')]
        rsp = self.app.assets.batch_import_files(assets)
        assert rsp['created'][0] == 'dd0KZtqyec48n1q1fniqVMV5yllhRRGx'

    @patch.object(BoonClient, 'get')
    def test_get_asset(self, get_patch):
        get_patch.return_value = {
            'id': 'abc13',
            'document': {
                'source': {
                    'path': 'gs://zorroa-dev-data/image/pluto.png'
                }
            }
        }
        asset = self.app.assets.get_asset('abc123')
        assert type(asset) == Asset
        assert asset.uri is not None
        assert asset.id is not None
        assert asset.document is not None

    @patch.object(BoonClient, 'upload_files')
    def test_batch_upload_directory(self, post_patch):
        post_patch.return_value = self.mock_import_result
        path = get_test_file('images/set01/toucan.jpg')

        called = []

        def callback(files, rsp):
            print(rsp)
            called.extend(files)

        rsp = self.app.assets.batch_upload_directory(
            os.path.dirname(path),
            callback=callback)

        assert 5 == rsp['file_count']
        assert 579485 == rsp['file_size']
        assert 1 == rsp['batch_count']
        assert called

    @patch.object(BoonClient, 'upload_files')
    def test_batch_upload_directory_file_types(self, post_patch):
        post_patch.return_value = self.mock_import_result
        path = get_test_file('images/set01/toucan.jpg')

        called = []

        def callback(files, rsp):
            called.extend(files)

        rsp = self.app.assets.batch_upload_directory(
            os.path.dirname(path),
            file_types=['doc'],
            callback=callback)

        assert 0 == rsp['file_count']
        assert 0 == rsp['file_size']
        assert 0 == rsp['batch_count']
        assert not called

    @patch.object(BoonClient, 'upload_files')
    def test_batch_upload_directory_batch_size(self, post_patch):
        post_patch.return_value = self.mock_import_result
        path = get_test_file('images/set01/toucan.jpg')

        rsp = self.app.assets.batch_upload_directory(
            os.path.dirname(path),
            batch_size=1)

        assert 5 == rsp['file_count']
        assert 579485 == rsp['file_size']
        assert 5 == rsp['batch_count']

    @patch.object(BoonClient, 'upload_files')
    def test_batch_upload_files(self, post_patch):
        post_patch.return_value = self.mock_import_result

        path = get_test_file('images/set01/toucan.jpg')
        assets = [FileUpload(path)]
        rsp = self.app.assets.batch_upload_files(assets)
        assert rsp['created'][0] == 'dd0KZtqyec48n1q1fniqVMV5yllhRRGx'

    @patch.object(BoonClient, 'post')
    def test_search_defaults(self, post_patch):
        post_patch.return_value = self.mock_search_result
        assets = self.app.assets.search().items
        assert 'https://i.imgur.com/SSN26nN.jpg' == assets[0].get_attr('source.path')

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_scroll_search_raises_on_no_scroll_id(self, post_patch, del_patch):
        post_patch.return_value = self.mock_search_result
        del_patch.return_value = {}
        with pytest.raises(BoonSdkException):
            for _ in self.app.assets.scroll_search():
                pass

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_scroll_search(self, post_patch, del_patch):
        scroll_result = copy.deepcopy(self.mock_search_result)
        scroll_result['_scroll_id'] = 'abc123'

        post_patch.side_effect = [scroll_result, {'hits': {'hits': []}}]
        del_patch.return_value = {}
        c = 0
        for _ in self.app.assets.scroll_search():
            c += 1
        assert c == 2

    @patch.object(BoonClient, 'post')
    def test_search_raw_response(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        rsp = self.app.assets.search(search=search)
        path = rsp.raw_response['hits']['hits'][0]['_source']['source']['path']
        assert path == 'https://i.imgur.com/SSN26nN.jpg'

    @patch.object(BoonClient, 'post')
    def test_search_with_filters(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        filt = TrainingSetFilter('abc123')
        rsp = self.app.assets.search(search=search, filters=filt)
        path = rsp.raw_response['hits']['hits'][0]['_source']['source']['path']
        assert path == 'https://i.imgur.com/SSN26nN.jpg'
        assert 'training_set' in post_patch.call_args[0][1]

    @patch.object(BoonClient, 'post')
    def test_search_iter(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        rsp = self.app.assets.search(search=search)
        path = rsp[0].get_attr('source.path')
        assert path == 'https://i.imgur.com/SSN26nN.jpg'
        assert 2 == rsp.size

        # Iterate the result to test iteration.
        count = 0
        for _ in rsp:
            count += 1
        assert count == 2

    @patch.object(BoonClient, 'post')
    def test_reprocess_search(self, post_patch):
        post_patch.return_value = {
            'assetCount': 101,
            'job': {
                'id': 'abc',
                'name': 'reprocess'
            }
        }
        search = {
            'query': {'match_all': {}}
        }
        rsp = self.app.assets.reprocess_search(search, ['boonai-labels'])
        assert 101 == rsp.asset_count
        assert 'abc' == rsp.job.id
        assert 'reprocess' == rsp.job.name

    @patch.object(BoonClient, 'delete')
    def test_delete_with_asset_object(self, del_patch):
        asset = Asset({'id': '123'})
        del_patch.return_value = {'success': True}
        res = self.app.assets.delete_asset(asset)
        args = del_patch.call_args_list
        uri = args[0][0][0]
        assert res is True
        assert '/api/v3/assets/123' == uri

    @patch.object(BoonClient, 'delete')
    def test_delete_with_asset_id(self, del_patch):
        del_patch.return_value = {'success': True}
        res = self.app.assets.delete_asset('123')
        args = del_patch.call_args_list
        uri = args[0][0][0]
        assert res is True
        assert '/api/v3/assets/123' == uri

    @patch.object(BoonClient, 'delete')
    def test_batch_delete_by_id(self, del_patch):
        q = {'assetIds': ["12345"]}
        del_patch.return_value = {
            "deleted": ["12345"]
        }
        res = self.app.assets.batch_delete_assets(["12345"])
        args = del_patch.call_args_list
        assert '/api/v3/assets/_batch_delete' == args[0][0][0]
        assert q == args[0][0][1]
        assert "12345" in res['deleted']

    @patch.object(BoonClient, 'delete')
    def test_batch_delete_by_assets(self, del_patch):
        assets = [Asset({'id': '12345'}), Asset({'id': '6789'})]
        self.app.assets.batch_delete_assets(assets)
        args = del_patch.call_args_list
        assert '/api/v3/assets/_batch_delete' == args[0][0][0]
        assert '12345' in args[0][0][1]['assetIds']
        assert '6789' in args[0][0][1]['assetIds']

    @patch.object(BoonClient, 'get')
    def test_download_file(self, get_patch):
        data = b'some_data'
        mockresponse = mock.Mock()
        mockresponse.content = data
        get_patch.return_value = mockresponse

        b = self.app.assets.download_file('assets/123/proxy/proxy123.jpg')
        assert 'some_data' == b.read().decode()

    @patch.object(BoonClient, 'get')
    def test_download_file_using_stored_file(self, get_patch):
        data = b'some_data'
        mockresponse = mock.Mock()
        mockresponse.content = data
        get_patch.return_value = mockresponse

        sf = StoredFile({"id": "assets/123/proxy/foo.jpg"})
        b = self.app.assets.download_file(sf)
        assert 'some_data' == b.read().decode()

    @patch.object(BoonClient, 'get')
    def test_download_file_to_file(self, get_patch):
        data = b'some_data'
        mockresponse = mock.Mock()
        mockresponse.content = data
        get_patch.return_value = mockresponse

        fd, path = tempfile.mkstemp(".jpg")
        size = self.app.assets.download_file(
            'assets/123/proxy/proxy123.jpg', dst_file=path)
        assert 9 == size

    @patch.object(BoonClient, 'get')
    def test_download_file_to_file_using_stored_file(self, get_patch):
        data = b'some_data'
        mockresponse = mock.Mock()
        mockresponse.content = data
        get_patch.return_value = mockresponse

        fd, path = tempfile.mkstemp(".jpg")
        sf = StoredFile({"id": "assets/123/proxy/foo.jpg"})
        size = self.app.assets.download_file(sf, path)
        assert 9 == size

    @patch.object(BoonClient, 'upload_files')
    def test_et_sim_hashes(self, upload_patch):
        upload_patch.return_value = ['ABC']
        path = get_test_file('images/set01/toucan.jpg')
        b = self.app.assets.get_sim_hashes(path)
        assert b == ['ABC']

    @patch.object(BoonClient, 'upload_files')
    def test_get_sim_hashes_file_handle(self, upload_patch):
        upload_patch.return_value = ['ABC']
        path = open(get_test_file('images/set01/toucan.jpg'), 'rb')
        b = self.app.assets.get_sim_hashes(path)
        assert b == ['ABC']

    @patch.object(BoonClient, 'upload_files')
    def test_get_sim_query(self, upload_patch):
        upload_patch.return_value = ['ABC']
        path = open(get_test_file('images/set01/toucan.jpg'), 'rb')
        b = self.app.assets.get_sim_query(path)
        assert b.hashes == ['ABC']

    @patch.object(BoonClient, 'put')
    def test_update_labels(self, put_patch):
        put_patch.return_value = {
            'type': 'asset',
            'op': '_batch_update_labels',
            'success': True
        }
        label1 = Dataset({"id": "abc123"}).make_label("test1")
        label2 = Dataset({"id": "abc123"}).make_label("test2")
        rsp = self.app.assets.update_labels(["12345"],
                                            add_labels=[label1], remove_labels=[label2])
        assert put_patch.return_value == rsp

    @patch.object(BoonClient, 'put')
    def test_batch_update_labels(self, put_patch):
        put_patch.return_value = {
            'type': 'asset',
            'op': '_batch_update_labels',
            'success': True
        }
        label1 = Dataset({"id": "abc123"}).make_label("test1")
        label2 = Dataset({"id": "abc123"}).make_label("test2")
        rsp = self.app.assets.batch_update_labels(["12345"],
                                                  add_label=[label1], remove_label=[label2])
        assert put_patch.return_value == rsp

    @patch.object(BoonClient, 'put')
    def test_batch_add_labels(self, put_patch):
        put_patch.return_value = {
            'type': 'asset',
            'op': '_batch_update_labels',
            'success': True
        }
        label1 = Dataset({"id": "abc123"}).make_label("test1")
        label2 = Dataset({"id": "abc123"}).make_label("test2")
        rsp = self.app.assets.batch_add_labels({"12345": label1, "abcd": label2})
        assert put_patch.return_value == rsp

    @patch.object(BoonClient, 'put')
    def test_batch_remove_labels(self, put_patch):
        put_patch.return_value = {
            'type': 'asset',
            'op': '_batch_update_labels',
            'success': True
        }
        label1 = Dataset({"id": "abc123"}).make_label("test1")
        label2 = Dataset({"id": "abc123"}).make_label("test2")
        rsp = self.app.assets.batch_remove_labels({"12345": label1, "abcd": label2})
        assert put_patch.return_value == rsp

    @patch.object(BoonClient, 'put')
    def test_update_custom_fields(self, put_patch):
        put_patch.return_value = {
            'success': True
        }
        rsp = self.app.assets.set_field_values(
            "12345", {"shoe": "nike"})
        assert put_patch.return_value == rsp

    @patch.object(BoonClient, 'put')
    def test_batch_custom_fields(self, put_patch):
        put_patch.return_value = {
            'success': True
        }
        req = {
            "asset1": {"foo": "bar"}
        }
        rsp = self.app.assets.batch_update_custom_fields(req)
        assert put_patch.return_value == rsp

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_scroll_search_clips(self, post_patch, del_patch):
        scroll_result = copy.deepcopy(self.mock_clip_search_result)
        scroll_result['_scroll_id'] = 'abc123'

        post_patch.side_effect = [scroll_result, {'hits': {'hits': []}}]
        del_patch.return_value = {}
        for clip in self.app.assets.scroll_search_clips('12345'):
            assert clip.asset_id == '12345'
            assert clip.timeline == 'foo'
            assert clip.track == 'bar'

    @patch.object(BoonClient, 'post')
    def test_apply_modules(self, post_patch):
        post_patch.return_value = {'id': "12345", "document": {"source": {"path": "/foo/bar.jpg"}}}
        res = self.app.assets.apply_modules('12345', ['boonai-face-detection'])
        assert '12345' == res.id
        assert "/foo/bar.jpg" == res.uri

    @patch.object(BoonClient, 'put')
    def test_set_languages(self, put):
        put.return_value = {}
        self.app.assets.set_languages('12345', ['en-US'])

    @patch.object(BoonClient, 'put')
    def test_label_search(self, put_patch):
        put_patch.return_value = {'count': 100}
        search = {"match_all": {}}
        label = boonsdk.Label("abc123", "cat")
        res = self.app.assets.label_search(search, label)
        assert res['count'] == 100

    @patch.object(BoonClient, 'send_data')
    def test_analyze_file(self, post_patch):
        post_patch.return_value = {
            'id': 'TRANSIENT',
            'document': {
                'analysis': {
                    'test': {
                        'predictions': [
                            {
                                'label': 'cat',
                                'score': 0.99
                            }
                        ]
                    }
                }
            }
        }

        with open(get_test_file('images/set01/toucan.jpg'), 'rb') as fp:
            asset = self.app.assets.analyze_file(fp, ['test'])
        assert asset.id == 'TRANSIENT'
