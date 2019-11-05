import os
import logging

from logging import LoggerAdapter

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
    processor.logger = DefaultFrameLogAdapter(processor.logger, {'asset': asset})


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
