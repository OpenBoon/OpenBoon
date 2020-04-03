from unittest import TestCase
from proxy.app.crawler import *


class TestCrawler(TestCase):
    def setUp(self):
        print("Setup")
        self.crawler = Crawler()

    def tearDown(self):
        print("Finish")

    def test_check_ext(self):
        assert self.crawler.check_ext("file.jpg") is True
