import tensorflow
import tensorflow.keras.applications.resnet_v2 as resnet_v2
from tensorflow.keras.applications.imagenet_utils import decode_predictions
from tensorflow.keras.layers import Input

from zmlpsdk import AssetProcessor
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from ..utils.keras import load_keras_image


class ZviLabelDetectionProcessor(AssetProcessor):
    """
    Performs image classification using Resnet152 and imagenet weights.
    """

    def init(self):
        tensorflow.config.set_visible_devices([], 'GPU')
        self.model = Resnet50ImageClassifier()

    def process(self, frame):
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        predictions = self.model.predict(proxy_path)

        analysis = LabelDetectionAnalysis()
        for label in predictions:
            analysis.add_label_and_score(label[1], label[2])

        asset.add_analysis('zvi-label-detection', analysis)


class Resnet50ImageClassifier:
    def __init__(self):
        self.model = resnet_v2.ResNet50V2(
            weights='imagenet', input_tensor=Input(shape=(224, 224, 3)))
        self.model.compile(loss='mse', optimizer='rmsprop')

    def predict(self, path):
        img = load_keras_image(path)
        result = self.model.predict(resnet_v2.preprocess_input(img))
        return decode_predictions(result)[0]
