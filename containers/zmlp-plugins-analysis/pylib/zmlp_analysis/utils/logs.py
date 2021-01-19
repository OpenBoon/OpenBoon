import logging

logger = logging.getLogger(__name__)


def log_backoff_exception(details):
    """
    Log an exception from the backoff library.

    Args:
        details (dict): The details of the backoff call.

    """
    logger.warning(
        'Waiting on quota {wait:0.1f} seconds afters {tries} tries'.format(**details))

