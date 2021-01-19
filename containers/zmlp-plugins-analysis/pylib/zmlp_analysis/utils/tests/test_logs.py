from zmlp_analysis.utils.logs import log_backoff_exception

import logging
logging.basicConfig(level=logging.INFO)


def test_log_backoff_exception():
    """Just make sure this won't throw"""
    log_backoff_exception({"tries": 1, "wait": 500})
