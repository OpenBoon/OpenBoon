import logging
import os

import requests
import google.cloud.logging
from google.cloud.logging_v2.handlers import CloudLoggingHandler

task_logger = logging.getLogger('task')
logger = logging.getLogger(__name__)


class LogFileRotator:
    """
    Handles swapping out different log file handlers for each task.
    """

    def __init__(self):
        """
        Create a new LogFileRotator
        """
        self.handler = None
        self.gc_logs_handler = None
        self.task = None

    def start_task_logging(self, task):
        """
        Create a FileHandler and attach it to the task logger.

        Args:
            task (dict): A task dictionary.

        """
        self.task = task

        # The log file is intended for customer use and thus
        # is not set to debug level.  Debug gives away underlying
        # software, versions, and internal communications protocol
        # which is a security risk.

        self.handler = logging.FileHandler(self.log_path)
        self.handler.setLevel(logging.INFO)
        task_logger.addHandler(self.handler)

        # If we're not in local dev setup the GCP log handler.
        if os.environ.get('ENVIRONMENT') != 'localdev':
            client = google.cloud.logging.Client()
            self.gc_logs_handler = CloudLoggingHandler(client)
            self.gc_logs_handler.setLevel(logging.INFO)
            task_logger.addHandler(self.gc_logs_handler)

        logger.info(f'Set up task log for {self.task_id}')

    def stop_task_logging(self):
        """
        Stop logging to the current log file handler and reset
        the task logger back to the default state.

        """
        if not self.handler:
            return

        try:
            logger.info(f'Closing task log for {self.task_id}')
            self.handler.close()
            task_logger.removeHandler(self.handler)
            self.gc_logs_handler.close()
            task_logger.removeHandler(self.gc_logs_handler)
            self.upload_log_file()
        except Exception:
            logger.exception(f'Failed to publish task log for {self.task_id}')
        finally:
            self.cleanup_log_file()
            self.handler = None
            self.task = None

    def upload_log_file(self):
        """
        Upload the log file to tasks log file location.  Throws if the
        log file cannot be uploaded.

        """
        size = str(os.path.getsize(self.log_path))
        logger.info(f'Uploading log file for ${self.task_id} size:{size}')

        with open(self.log_path, 'rb') as fp:
            rsp = requests.put(self.task['logFile'],
                               headers={
                                   'Content-Type': 'text/plain',
                                   'Content-Length': size
                               },
                               data=fp)
            rsp.raise_for_status()

    def cleanup_log_file(self):
        """
        Remove the task log file.
        """
        try:
            os.unlink(self.log_path)
        except Exception as e:
            logger.warning(f'Failed to delete log file: {self.log_path}', e)

    @property
    def log_path(self):
        """
        The path to the tmp log file.
        """
        return f'/tmp/{self.task_id}.log'

    @property
    def task_id(self):
        """
        The task id.
        """
        return self.task['taskId']
