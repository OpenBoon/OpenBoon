import os
import logging

from logging import LoggerAdapter

import google.cloud.logging
from google.cloud.logging import Client
from google.cloud.logging.handlers import CloudLoggingHandler

__all__ = [
    "reset_logger_format",
    "setup_logger_format"
]


def reset_logger_format():
    """Resets log formatting back to the environment default."""
    setup_logger_format()


def setup_logger_format(processor=None, asset=None, level=logging.INFO):
    """Set up custom log formatting for specific runtime environments where
    a log aggregation system is expecting specific formatting or tags.  This
    function automatically detects the environment (GCP or on-prem currently).

    Args:
        processor(zsdk.processor.Processor): If provided, the processor name is attached to the log.
        asset(zsdk.document.asset.Asset): If given then labels for asset id and source
         path will be added to the logs.
        level(int): Logging level to set the handler to if it does not already exist.

    """
    if os.environ.get('STACKDRIVER_LOGGING'):
        format_logs_for_stackdriver(processor, asset, level)
    elif processor:
        processor.logger = DefaultFrameLogAdapter(processor.logger, {'asset': asset})


def format_logs_for_stackdriver(processor, asset, level):
    """Enables a Google Cloud Platform Stackdriver logging handler if the STACKDRIVER_LOGGING
    environment variable evaluates to True. This sends all of the logs to stackdriver in
    an easy to read format with the correct log levels. The logs also have labels in the
    metadata that can be searched in easily. The metadata added is labels.zorroa_task_id,
    labels.zorroa_job_id, labels.zorroa_organization_id and labels.zorroa_asset_id. If the
    stackdriver handler already exists then the it will be updated with the correct labels.

    Args:
        processor(zsdk.processor.Processor): If provided, the processor name is attached to the log.
        asset(zsdk.document.asset.Asset): If given then labels for asset id and source
         path will be added to the logs.
        level(int): Logging level to set the handler to if it does not already exist.

    """
    labels = {'analyst_task_id': os.environ.get('ZORROA_TASK_ID'),
              'analyst_job_id': os.environ.get('ZORROA_JOB_ID'),
              'analyst_organization_id': os.environ.get('ZORROA_ORGANIZATION_ID')}
    if asset:
        labels['analyst_asset_id'] = str(asset.id)
        labels['analyst_file_name'] = str(asset.source_path)
    if processor:
        labels['analyst_processor'] = processor.__class__.__name__

    labels = {k: v for k, v in labels.iteritems() if v}
    for handler in logging.getLogger().handlers:
        if isinstance(handler, CloudLoggingHandler):
            handler.labels = labels
            return
    _add_stackdriver_log_handler(labels, level)
    logging.getLogger().debug('Stackdriver log handler enabled.')


def _add_stackdriver_log_handler(labels, level):
    """Adds a stackdriver log handler. This method is extracted out test seam to allow
    easy unit testing. It is unlikely using this function directly is the best idea.

    Args:
        labels(dict): Labels to add to stackdriver logs.
        level(int): Log level to set for the handler.

    """
    client = Client()
    handler = CloudLoggingHandler(client)
    handler.labels = labels
    google.cloud.logging.handlers.setup_logging(handler, log_level=level)


class DefaultFrameLogAdapter(LoggerAdapter):
    """Adds Asset and task based information to every logger line using this adapter.
    """
    def process(self, msg, kwargs):
        log_line = []
        args = []

        asset = self.extra.get('asset')
        if asset:
            log_line.append("zorroa.assetId='{}'")
            args.append(asset.id)

        task_id = os.environ.get('ZORROA_TASK_ID')
        if task_id:
            log_line.append("zorroa.taskId='{}'")
            args.append(task_id)

        log_line.append(':: {}')
        args.append(msg)

        line = " ".join(log_line)

        return line.format(*args), kwargs

    def warn(self, msg, *args, **kwargs):
        """
        Delegate a warning call to the underlying logger, after adding
        contextual information from this adapter instance.

        Args:
            msg (str): The message to log.
            *args (mixed): args to pass to logger.
            **kwargs: (mixed) kwargs to pass to logger.

        """
        # The 'warn' function is not implemented on the base LoggerAdapter class for some reason.
        # Due to the fact it does exist on Logger instances and is commonly used, we've added
        # the warn method here which calls through to warning().
        return self.warning(msg, *args, **kwargs)
