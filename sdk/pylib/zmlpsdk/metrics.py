import time
import logging

logger = logging.getLogger(__name__)


class StopWatch:
    """
    The StopWatch class is a ContextManager which simply logs how
    long an operation takes.
    """
    def __init__(self, name):
        """
        Create a new stopwatch.

        Args:
            name(str): The name of the operation we're going to time.
        """
        self.name = name
        self.start = None

    def __enter__(self):
        self.start = time.time()

    def __exit__(self, type, value, traceback):
        t = round(time.time() - self.start, 3)
        logger.info(f'Timed op: [{self.name}] took: [{t}s]')
