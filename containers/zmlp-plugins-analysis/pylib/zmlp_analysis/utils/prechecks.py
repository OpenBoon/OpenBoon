"""
A set of functions for doing common pre-condition checks.
"""
import logging

logger = logging.getLogger(__name__)


class Prechecks:
    """
    A class for defining various hard coded configuration values.
    """

    max_video_length = 7200
    """The maximum length of a video to process in seconds"""

    @classmethod
    def is_valid_video_length(cls, asset):
        if not asset.get_attr('media.length') \
                or asset.get_attr('media.length') > cls.max_video_length:
            logger.warning(
                'Skipping, video is longer than {} seconds.'.format(cls.max_video_length))
            return False
        return True
