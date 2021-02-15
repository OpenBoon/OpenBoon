import logging
import time
from unittest import TestCase

from boonflow.metrics import StopWatch

logging.basicConfig(level=logging.DEBUG)


class TestMetrics(TestCase):

    def test_stopwtach_class(self):
        with StopWatch("test"):
            time.sleep(0.25)
