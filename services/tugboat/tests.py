import os
import unittest


class ServerTests(unittest.TestCase):
    def setUp(self):
        os.environ['GCLOUD_PROJECT'] = 'localdev'

    def tearDown(self):
        del os.environ['GCLOUD_PROJECT']
