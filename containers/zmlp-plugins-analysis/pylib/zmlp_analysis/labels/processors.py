import numpy as np
import tensorflow
from tensorflow.keras.applications.imagenet_utils import decode_predictions
from tensorflow.keras.applications.resnet_v2 import ResNet152V2, preprocess_input
from tensorflow.keras.layers import Input
from tensorflow.keras.preprocessing.image import load_img, img_to_array

from zmlpsdk import AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path
from zmlpsdk.analysis import LabelDetectionAnalysis


class ZviLabelDetectionProcessor(AssetProcessor):
    """
    Performs image classification using Resnet152 and imagenet weights.
    """
    def init(self):
        tensorflow.config.set_visible_devices([], 'GPU')
        self.model = ResNet152V2(weights='imagenet', input_tensor=Input(shape=(224, 224, 3)))
        self.model.compile(loss='mse', optimizer='rmsprop')

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 0)
        img = self.load_image(p_path)
        predictions = self.model.predict(preprocess_input(img))

        analysis = LabelDetectionAnalysis()
        for label in decode_predictions(predictions)[0]:
            analysis.add_label_and_score(label[1], label[2])

        asset.add_analysis('zvi-label-detection', analysis)

    def load_image(self, path):
        """
        Load the given image and prepare it for use by Tensorflow.

        Args:
            path (str): The path to the file to load.

        Returns:
            numpy array: an array of bytes for Tensorflow use.
        """
        img = load_img(
            path,
            grayscale=False,
            color_mode='rgb',
            target_size=(224, 224),
            interpolation='nearest'
        )

        numpy_image = img_to_array(img)
        return np.expand_dims(numpy_image, axis=0)
