from ..util import as_id


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

    def get_webvtt(self, asset, dst_file=None):
        """
        Get all clip data as a webvtt file.

        Args:
            asset (Asset): The asset or unique Id.
            dst_file (mixed): An optional writable file handle or path to file.

        Returns:
            mixed: The text of the webvtt or the size of the written file.
        """
        aid = as_id(asset)
        rsp = self.app.client.get(f'/api/v3/assets/{aid}/clips/all.vtt', is_json=False)
        return self.__handle_webvtt(rsp, dst_file)

    def get_webvtt_by_timeline(self, asset, timeline, dst_file=None):
        """
        Get a specific timeline as a webvtt file.

        Args:
            asset (Asset): The asset or unique Id.
            timeline: (str): The name of the timeline
            dst_file (mixed): An optional writable file handle or path to file.

        Returns:
            mixed: The text of the webvtt or the size of the written file.

        """
        aid = as_id(asset)
        rsp = self.app.client.get(
            f'/api/v3/assets/{aid}/clips/timelines/{timeline}.vtt', is_json=False)
        return self.__handle_webvtt(rsp, dst_file)

    def __handle_webvtt(self, rsp, dst_file):
        """
        Handle a webvtt file response.

        Args:
            rsp (Response): A response from requests.
            dst_file (mixed): An optional file path or file handle.

        Returns:
            (mixed): Return the content itself or the content size if written to file.
        """
        if dst_file:
            if isinstance(dst_file, str):
                with open(dst_file, 'w') as fp:
                    fp.write(rsp.content.decode())
                return len(rsp.content)
            else:
                dst_file.write(rsp.content.decode())
                return len(rsp.content)
        else:
            return rsp.content.decode()
