import logging
import os
import threading

# This module should be loaded before anything else to ensure the default
# logging class is AssetLogger, otherwise you may see some formatting
# exceptions in the log output.


class AssetLogger(logging.Logger):
    """
    A logging.Logger subclass that uses the 'extra' feature to pass
    a thread local Asset id to the log formatter.   This ensures all
    output from loggers contain an Asset Id.

    """

    # Used for storing the asset_id being processed by the current thread.
    local_data = threading.local()

    # The default asset Id if one does not exist.
    default_asset_id = "NONE"

    def _log(self, level, msg, args, exc_info=None, extra=None):
        extra2 = {
            "asset_id": getattr(self.local_data, "asset_id", self.default_asset_id)
        }
        super(AssetLogger, self)._log(level, msg, args, exc_info, extra2)

    @classmethod
    def set_asset_id(cls, asset_id):
        cls.local_data.asset_id = asset_id

    @classmethod
    def clear_asset_id(cls):
        cls.local_data.asset_id = "NONE"


# Override the default logger class with our AssetLogger
logging.setLoggerClass(AssetLogger)


def setup_logging():
    """
    Setup the root logging config with logging.basicConfig

    """
    log_format = '%(name)-15s [asset=%(asset_id)s] %(message)s'
    if os.environ.get("BOONAI_DEBUG"):
        logging.basicConfig(level=logging.DEBUG, format=log_format)
    else:
        logging.basicConfig(level=logging.INFO, format=log_format)
