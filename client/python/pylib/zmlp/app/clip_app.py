from ..util import as_id, as_id_collection
from ..search import VideoClipSearchResult, VideoClipSearchScroller
from ..entity import VideoClip


class VideoClipApp:
    """
    An App instance for managing Jobs. Jobs are containers for async processes
    such as data import or training.
    """
    def __init__(self, app):
        self.app = app

    def create_clip(self, asset, timeline, track, start, stop, content):
        """
        Create a new clip.  If a clip with the same metadata already exists it will
        simply be replaced.

        Args:
            asset (Asset): The asset or its unique Id.
            timeline (str): The timeline name for the clip.
            track (str): The track name for the clip.
            start (float): The starting point for the clip in seconds.
            stop (float): The ending point for the clip in seconds.
            content (str): The content of the clip.

        Returns:
            Clip: The clip that was created.
        """
        body = {
            "assetId": as_id(asset),
            "timeline": timeline,
            "track": track,
            "start": start,
            "stop": stop,
            "content": content
        }
        return VideoClip(self.app.client.post('/api/v1/clips', body))

    def create_clips(self, timeline):
        """
        Batch create clips using a TimelineBuilder.

        Args:
            timeline: (TimelineBuilder): A timeline builder.

        Returns:
            dict: A status dictionary
        """
        return self.app.client.post('/api/v1/clips/_timeline', timeline)

    def get_webvtt(self,
                   asset,
                   dst_file=None,
                   timeline=None,
                   track=None,
                   content=None):
        """
        Get all clip data as a WebVTT file and filter by specified options.

        Args:
            asset (Asset): The asset or unique Id.
            timeline: (str): A timeline name or collection of timeline names.
            track: (str): A track name or collection of track names.
            content (str): A content string to match.
            dst_file (mixed): An optional writable file handle or path to file.

        Returns:
            mixed: The text of the webvtt or the size of the written file.
        """
        body = {
            'assetId': as_id(asset),
            'timelines': as_id_collection(timeline),
            'tracks': as_id_collection(track),
            'content': as_id_collection(content)
        }
        rsp = self.app.client.post('/api/v1/clips/_webvtt', body=body, is_json=False)
        return self.__handle_webvtt(rsp, dst_file)

    def scroll_search(self, search=None, timeout="1m"):
        """
        Perform a VideoClip scrolled search using the ElasticSearch query DSL.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute
            timeout (str): The scroll timeout.  Defaults to 1 minute.
        Returns:
            VideoClipSearchScroller - an VideoClipSearchScroller instance which can be used as
            a generator for paging results.

        """
        return VideoClipSearchScroller(self.app, search, timeout)

    def search(self, search=None):
        """
        Perform an VideoClip search using the ElasticSearch query DSL.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute.
        Returns:
            VideoClipSearchResult - A VideoClipSearchResult instance.
        """
        return VideoClipSearchResult(self.app, search)

    def get_clip(self, id):
        """
        Get a VideoClip by unique Id.

        Args:
            id (str): The VideoClip or its unique Id.

        Returns:
            VideoClip: The clip with the given Id.
        """
        return VideoClip(self.app.client.get(f'api/v1/clips/{id}'))

    def download_file(self, stored_file, dst_file=None):
        """
        Download given file and store results in memory, or optionally
        a destination file.  The stored_file ID can be specified as
        either a string like "assets/<id>/proxy/image_450x360.jpg"
        or a StoredFile instance can be used.

        Args:
            stored_file (mixed): The StoredFile instance or its ID.
            dst_file (str): An optional destination file path.

        Returns:
            io.BytesIO instance containing the binary data or if
                a destination path was provided the size of the
                file is returned.

        """
        return self.app.client.download_file(stored_file, dst_file)

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
