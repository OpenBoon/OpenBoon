import logging
import os
import unittest
from unittest.mock import patch

from analyst.logs import LogFileRotator

logging.basicConfig(level=logging.INFO)


class MockResponse:
    """
    A mock requests response.
    """

    def raise_for_status(self):
        pass


class LogFileRotatorTests(unittest.TestCase):
    test_task = {
        'taskId': 'CEAD09A8-8FA5-416A-AC5E-68806325AA1D',
        'logFile': "http://localhost"
    }

    def test_start_logging(self):
        r = LogFileRotator()
        r.start_task_logging(self.test_task)
        self.assertEquals(self.test_task['taskId'], r.task_id)
        self.assertIsNotNone(r.task)
        self.assertIsNotNone(r.handler)

    @patch('requests.put')
    def test_stop_logging(self, put_patch):
        put_patch.return_value = MockResponse()
        r = LogFileRotator()
        r.start_task_logging(self.test_task)
        log_path = r.log_path
        r.stop_task_logging()
        self.assertIsNone(r.task)
        self.assertIsNone(r.handler)
        self.assertFalse(os.path.exists(log_path))

    def test_check_log_file(self):
        r = LogFileRotator()
        r.start_task_logging(self.test_task)
        log_path = r.log_path
        logger = logging.getLogger('task')
        logger.info("hello")

        log = open(log_path, "r").read()
        self.assertTrue("hello" in log)
