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


class GCLogger:
    def emit(self, record):
        return


class GCLogsClient:
    def logger(self, topic):
        return GCLogger()


class LogFileRotatorTests(unittest.TestCase):
    test_task = {
        'taskId': 'CEAD09A8-8FA5-416A-AC5E-68806325AA1D',
        'logFile': "http://localhost"
    }

    @patch('google.cloud.logging.Client')
    def test_start_logging(self, gc_log_patch):
        r = LogFileRotator()
        gc_log_patch.return_value = GCLogsClient()
        r.start_task_logging(self.test_task)
        self.assertEquals(self.test_task['taskId'], r.task_id)
        self.assertIsNotNone(r.task)
        self.assertIsNotNone(r.handler)

    @patch('google.cloud.logging.Client')
    @patch('requests.put')
    def test_stop_logging(self, put_patch, gc_log_patch):
        put_patch.return_value = MockResponse()
        gc_log_patch.return_value = GCLogsClient()
        r = LogFileRotator()
        r.start_task_logging(self.test_task)
        log_path = r.log_path
        r.stop_task_logging()
        self.assertIsNone(r.task)
        self.assertIsNone(r.handler)
        self.assertFalse(os.path.exists(log_path))

    @patch('google.cloud.logging.Client')
    def test_check_log_file(self, gc_log_patch):
        gc_log_patch.return_value = GCLogsClient()
        r = LogFileRotator()
        r.start_task_logging(self.test_task)
        log_path = r.log_path
        logger = logging.getLogger('task')
        logger.info("hello")

        log = open(log_path, "r").read()
        self.assertTrue("hello" in log)
