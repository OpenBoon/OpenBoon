import requests

from boonai_analysis.utils.prechecks import Prechecks
from boonflow import Argument, file_storage, proxy, clips, video, Prediction
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.proxy import get_proxy_level_path
from ..custom.base import CustomModelProcessor


class TorchModelArchiveClassifier(CustomModelProcessor):

    def __init__(self):
        super(TorchModelArchiveClassifier, self).__init__()
        self.add_arg(Argument("endpoint", "list", required=True))

    def init(self):
        """Init constructor """
        # get model by model id
        self.load_app_model()

    def process(self, frame):
        asset = frame.asset
        if asset.get_attr('media.type') == "video":
            self.process_video(frame.asset)
        else:
            self.process_image(frame.asset)

    def process_image(self, asset):
        proxy_path = get_proxy_level_path(asset, 1)
        predictions = self.predict(proxy_path)

        analysis = LabelDetectionAnalysis(min_score=0.1)
        for label in predictions:
            analysis.add_label_and_score(label[0], label[1])

        asset.add_analysis(self.app_model.module_name, analysis)

    def predict(self, path):
        """
        Call the model to make predictions.

        Args:
            path (str): Path to the proxy image.

        Returns:
            list: A list of tuples containing predictions

        """
        with open(path, 'rb') as fp:
            preds = requests.post(self.arg_value('endpoint'), data=fp).json()
        return [(k, v) for k, v in preds.items()]

    def process_video(self, asset):
        """
        Process a video asset.

        Args:
            asset (Asset): An Asset instance.
        """
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if not Prechecks.is_valid_video_length(asset):
            return

        video_proxy = proxy.get_video_proxy(asset)
        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)

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
        analysis = LabelDetectionAnalysis(collapse_labels=True, min_score=0.1)

        for time_ms, path in extractor:
            results = [Prediction(r[0], r[1]) for r in self.predict(path)]
            clip_tracker.append_predictions(time_ms, results)
            analysis.add_predictions(results)
        return analysis, clip_tracker
