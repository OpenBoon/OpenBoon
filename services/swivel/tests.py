import os
import queue
import threading
import unittest
import time
import unittest.mock as mock

import jwt

import server

mock_msg = {
    'url': 'http://foobar:5000',
    'asset_id': '12345',
    'project_id': 'abcdefg',
    'secret_key': 'terminator2000',
    'trigger': 'ASSET_ANALYZED',
    'data': b'{}'
}


class ServerTests(unittest.TestCase):
    def setUp(self):
        os.environ['GCLOUD_PROJECT'] = 'localdev'

    def tearDown(self):
        del os.environ['GCLOUD_PROJECT']

    @mock.patch('server.call_webhook')
    def test_webhook_worker(self, call_patch):
        q = queue.Queue()
        t = threading.Thread(target=server.webhook_worker, args=(q,))
        t.daemon = True
        t.start()

        q.put(mock_msg)
        while q.qsize() != 0:
            time.sleep(1)

        call_args = call_patch.call_args_list[0][0][0]
        assert mock_msg['url'] == call_args['url']

    @mock.patch('server.requests.post')
    def test_call_webhook(self, post_patch):
        class Response:
            status_code = 999

        post_patch.return_value = Response()
        rsp = server.call_webhook(mock_msg)
        assert rsp.status_code == 999

    def test_genertate_token(self):
        token = server.generate_token(mock_msg)
        claims = jwt.decode(token, 'terminator2000', algorithms=['HS256'])
        assert claims['asset_id'] == '12345'

    def test_health(self):
        health = server.health()
        assert health == ('OK', 200)
