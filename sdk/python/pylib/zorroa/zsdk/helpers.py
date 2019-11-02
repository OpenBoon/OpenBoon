from .processor import ProcessorHelper


class ClipifierError(Exception):
    pass


class AbstractClipifier(ProcessorHelper):
    """Abstract class all Clipifiers should inherit from. Clipifiers are used by the
    video ingestor to break videos into individual clips using various methods. When
    implementing a Clipifier the _get_clips method should be overridden.

    Args:
        logger (Logger): Logger the Clipifier should use.

    """
    def __init__(self, logger, minimum_clip_length=1.0):
        super(AbstractClipifier, self).__init__(logger)
        self.minimum_clip_length = minimum_clip_length

    def get_clips(self, asset):
        """Returns a list of clips for the asset described as start and stop points in
        seconds.

        Args:
            asset (Asset): Video asset to break into individual clips.

        Returns:
            list<tuple>: Clips expressed in the form (start, stop). The start and stop are
             measured in seconds.

        """
        clips = self._get_clips(asset)

        # Validates the return of the overridden method in concrete classes.
        for clip in clips:
            format_validation_message = ('Clipifier did not return clips in a valid form.'
                                         ' Invalid: %s' % str(clip))
            if not isinstance(clip, tuple):
                raise TypeError(format_validation_message)
            if len(clip) != 2:
                raise IndexError(format_validation_message)
            for seconds in clip:
                if not isinstance(seconds, float):
                    raise TypeError(format_validation_message)

        return clips

    def _get_clips(self, asset):
        """This method is wrapped by the get_clips method and must be overridden by all
        concrete classes. This method is responsible for returning a list of tuples in the
        form (start_seconds, stop_seconds) that specify the in and out points for clips
        parsed from the asset's video file.

        """
        raise NotImplementedError

