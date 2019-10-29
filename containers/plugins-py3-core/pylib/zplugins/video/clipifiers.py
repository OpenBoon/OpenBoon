import backoff
import subprocess32 as subprocess
from zsdk.processor import ProcessorHelper
from zsdk.exception import UnrecoverableProcessorException
from google.cloud import videointelligence
from google.cloud import storage
from google.api_core.exceptions import ResourceExhausted


class ClipifierError(Exception):
    pass


class AbstractClipifier(ProcessorHelper):
    """Abstract class all Clipifiers should inherit from. Clipifiers are used by the
    video ingestor to break videos into individual clips using various methods. When
    implementing a Clipifier the _get_clips method should be overridden.

    Args:
        logger (Logger): Logger the Clipifier should use.

    """
    def __init__(self, logger, minimum_clip_length=1.0, gcp_temp_bucket_name=''):
        super(AbstractClipifier, self).__init__(logger)
        self.minimum_clip_length = minimum_clip_length
        self.gcp_temp_bucket_name = gcp_temp_bucket_name

        if len(self.gcp_temp_bucket_name):
            self.gcp_storage_client = storage.Client()
            self.gcp_storage_bucket = self.gcp_storage_client.get_bucket(self.gcp_temp_bucket_name)
            self.gcp_video_client = videointelligence.VideoIntelligenceServiceClient()

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


class FFProbeKeyframeClipifier(AbstractClipifier):
    """Uses FFProbe to create clips based on keyframes."""
    def _get_clips(self, asset):
        movie_file = asset.get_local_source_path()
        minimum_clip_length = float(self.minimum_clip_length)

        # Get the duration.
        duration_command = ['ffprobe', '-v', 'error', '-show_entries', 'format=duration',
                            '-of', 'default=noprint_wrappers=1:nokey=1', movie_file]
        duration = subprocess.check_output(duration_command)
        duration = round(float(duration), 3)

        # Get the keyframes.
        keyframe_command = ('ffprobe -show_frames -of compact=p=0 -show_entries '
                            'frame=pkt_pts_time,pict_type -f lavfi '
                            'movie=' + movie_file + ',select=gt(scene\\,0.1)')
        self.logger.info('FFPROBE COMMAND: %s' % keyframe_command)
        p = subprocess.Popen(keyframe_command.split(), stderr=subprocess.PIPE,
                             stdout=subprocess.PIPE)
        previous_seconds = 0.0
        clips = []
        while True:
            line = p.stdout.readline()
            if not line:
                break
            line = line.strip()
            if not line.startswith('pkt_pts_time'):
                continue
            self.logger.info('PROCESSING LINE: %s' % line)
            current_seconds = round(float(line.split('|')[0].split('=')[1]), 3)

            # Skip keyframe if the clip created will be too short
            if current_seconds - previous_seconds < minimum_clip_length:
                continue

            clips.append((previous_seconds, current_seconds))
            self.logger.info(current_seconds)
            previous_seconds = current_seconds
        clips.append((previous_seconds, duration))
        try:
            p.wait()
        except OSError:
            self.logger.warning('Exception thrown waiting on process to complete.')
        return clips


class GoogleVideoIntelligenceClipifier(AbstractClipifier):
    def _get_clips(self, asset):
        if not len(self.gcp_temp_bucket_name):
            raise UnrecoverableProcessorException("Parameter gcp_temp_bucket_name must be set to "
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
