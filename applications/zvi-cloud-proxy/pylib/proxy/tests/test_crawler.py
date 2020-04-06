import unittest

from proxy.app.crawler import Crawler
from unittest.mock import patch


class TestCrawler(unittest.TestCase):
    def setUp(self):
        self.crawler = Crawler("./properties-test.yaml")

    def test_check_ext(self):
        assert self.crawler.check_ext("file.jpg") is True
        assert self.crawler.check_ext("file.png") is True
        assert self.crawler.check_ext("file.mov") is False
        assert self.crawler.check_ext("file.py") is False
        assert self.crawler.check_ext("file") is False

    @patch("proxy.app.crawler.Crawler.upload_batch")
    def test_upload_batch(self, mock):
        json = '{"failed": [], "created": [], "exists": ["1uhJgrGM-kMF0L31RxXsXHV3KYFjP9TT"], "jobId": ' \
                   '"69bdfb20-784b-11ea-9d81-9a0c79a4c299", "totalUpdated": 1}'
        mock.return_value = json

        response = self.crawler.upload_batch("./file.png")
        self.assertEqual(response, json)

    def tearDown(self):
        print("Finish")
