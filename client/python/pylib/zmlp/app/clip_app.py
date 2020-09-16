
class ClipApp:
    """
    An App instance for managing Jobs. Jobs are containers for async processes
    such as data import or training.
    """
    def __init__(self, app):
        self.app = app

    def create_clips_from_timeline(self, timeline):
        """
        Batch create clips using a TimelineBuilder.

        Args:
            timeline: (TimelineBuilder): A timeline builder.

        Returns:
            dict: A status dictionary
        """
        return self.app.client.post('/api/v1/clips/_timeline', timeline)
