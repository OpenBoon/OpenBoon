from zmlpsdk import AssetProcessor, Argument, FileTypes, file_storage, proxy, clips, video
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlp_analysis.aws.faces import RekognitionFaceDetection

MAX_LENGTH_SEC = 120


class RekognitionVideoFaceDetection(AssetProcessor):
    """Get labels for a video using AWS Rekognition """

    file_types = FileTypes.videos

    namespace = 'aws-face-detection'

    def __init__(self, extract_type=None, reactor=None):
        super(RekognitionVideoFaceDetection, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.extract_type = extract_type
        self.reactor = reactor
        self.image_client = None

    def init(self):
        self.image_client = RekognitionFaceDetection()
        self.image_client.init()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if final_time > MAX_LENGTH_SEC:
            self.logger.warning(
                'Skipping, video is longer than {} seconds.'.format(self.max_length_sec))
            return

        video_proxy = proxy.get_video_proxy(asset)

        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)

        if self.extract_type == 'time':
            extractor = video.TimeBasedFrameExtractor(local_path)
        else:
            extractor = video.ShotBasedFrameExtractor(local_path)

        clip_tracker = clips.ClipTracker(asset, self.namespace)

        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker, self.image_client)
        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(timeline)

    def set_analysis(self, extractor, clip_tracker, proc):
        """ Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker
            proc: Amazon Rekog image Client

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True)

        for time_ms, path in extractor:
            predictions = proc.predict(path)
            labels = [pred[0] for pred in predictions]
            clip_tracker.append(time_ms, labels)
            for ls in predictions:
                analysis.add_label_and_score(ls[0], ls[1])

        return analysis, clip_tracker

    def emit_status(self, msg):
        """
        Emit a status back to the Archivist.

        Args:
            msg (str): The message to emit.

        """
        if not self.reactor:
            return
        self.reactor.emit_status(msg)
