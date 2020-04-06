import unittest

from proxy.app.crawler import *


class TestCrawler(unittest.TestCase):
    def setUp(self):
        self.crawler = Crawler("./properties-test.yaml")

    def test_check_ext(self):
        assert self.crawler.check_ext("file.jpg") is True
        assert self.crawler.check_ext("file.png") is True
        assert self.crawler.check_ext("file.mov") is False
        assert self.crawler.check_ext("file.py") is False
        assert self.crawler.check_ext("file") is False

    def tearDown(self):
        print("Finish")
