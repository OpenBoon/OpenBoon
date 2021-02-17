
from boonflow import AssetProcessor, Argument, FileTypes, file_storage, proxy, clips, video
from boonflow.analysis import LabelDetectionAnalysis
from boonai_analysis.custom.labels import TensorflowTransferLearningClassifier
from boonai_analysis.utils.prechecks import Prechecks


class VideoTensorflowTransferLearningClassifier(AssetProcessor):
    """Classifier for retrained saved model on video """

    file_types = FileTypes.videos

    def __init__(self, extract_type=None):
        super(VideoTensorflowTransferLearningClassifier, self).__init__()

        self.add_arg(
            Argument("model_id", "str", required=True, toolTip="The model Id")
        )
        self.extract_type = extract_type

        self.tf_client = None
        self.app_model = None
        self.trained_model = None
        self.labels = None

    def init(self):
        """Init constructor """
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))
        self.tf_client = TensorflowTransferLearningClassifier()
        self.tf_client.init()

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        Returns:
            None
        """
        asset = frame.asset
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if not Prechecks.is_valid_video_length(asset):
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

        clip_tracker = clips.ClipTracker(asset, self.app_model.module_name)
        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker)
        asset.add_analysis(self.app_model.module_name, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(asset, timeline)

    def set_analysis(self, extractor, clip_tracker):
        """ Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True, min_score=0.01)

        for time_ms, path in extractor:
            clip_tracker.append(time_ms, self.tf_client.labels)
            results = self.tf_client.predict(path)
            [analysis.add_label_and_score(r[0], r[1]) for r in results]

        return analysis, clip_tracker
