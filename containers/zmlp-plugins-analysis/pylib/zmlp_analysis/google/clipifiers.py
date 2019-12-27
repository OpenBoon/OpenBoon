
import backoff
from zmlp.analysis import AbstractClipifier
from zmlp.analysis import ZmlpFatalProcessorException

from google.cloud import videointelligence
from google.cloud import storage
from google.api_core.exceptions import ResourceExhausted


class GoogleVideoIntelligenceClipifier(AbstractClipifier):

    def __init__(self, logger, minimum_clip_length=1.0, gcp_temp_bucket_name=None):
        super(GoogleVideoIntelligenceClipifier, self).__init__(logger, minimum_clip_length)
        self.gcp_temp_bucket_name = gcp_temp_bucket_name
        self._setup_gcp_video_client()

    def _setup_gcp_video_client(self):
        self.gcp_storage_client = storage.Client()
        self.gcp_storage_bucket = self.gcp_storage_client.get_bucket(self.gcp_temp_bucket_name)
        self.gcp_video_client = videointelligence.VideoIntelligenceServiceClient()

    def _get_clips(self, asset):
        if not len(self.gcp_temp_bucket_name):
            raise ZmlpFatalProcessorException(
                "Parameter gcp_temp_bucket_name must be set to "
                "use GoogleVideoIntelligenceClipifier")

        movie_file = asset.get_local_source_path()
        self._copy_file_to_bucket(movie_file)
        shot_info = self._process_remotely(self._get_gs_url('/'+self._get_remote_path(movie_file)))
        clips = self._extract_clips(shot_info)
        self._remove_file_from_bucket(movie_file)
        return clips

    def _get_remote_path(self, local_path):
        return 'clipifier{}'.format(local_path)

    def _get_gs_url(self, local_path):
        return 'gs://{}{}'.format(self.gcp_temp_bucket_name, local_path)

    def _copy_file_to_bucket(self, local_path):
        print('\tUploading {} to {}'.format(local_path, self._get_gs_url(local_path)))
        blob = self.gcp_storage_bucket.blob(self._get_remote_path(local_path))
        blob.upload_from_filename(local_path)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _process_remotely(self, gs_url):
        features = [videointelligence.enums.Feature.SHOT_CHANGE_DETECTION]
        print('\tSubmitting {} to GCP for processing...'.format(gs_url))
        operation = self.gcp_video_client.annotate_video(input_uri=gs_url, features=features)
        shot_info = operation.result(timeout=60 * 60)
        return shot_info

    def _extract_clips(self, shot_info):
        clips = []
        for i, shot in enumerate(shot_info.annotation_results[0].shot_annotations):
            start_time = (shot.start_time_offset.seconds +
                          shot.start_time_offset.nanos / 1e9)
            end_time = (shot.end_time_offset.seconds +
                        shot.end_time_offset.nanos / 1e9)
            print('\tShot {}: {} to {}'.format(i, start_time, end_time))
            clips.append((start_time, end_time))
            self.logger.info(start_time)
        return clips

    def _remove_file_from_bucket(self, local_path):
        blob = self.gcp_storage_bucket.blob(self._get_remote_path(local_path))
        blob.delete()
