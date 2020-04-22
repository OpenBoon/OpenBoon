import numpy as np
import tensorflow
import tensorflow.keras.applications.resnet_v2 as resnet_v2
import tensorflow.keras.applications.vgg16 as vgg_16
from tensorflow.keras.applications.imagenet_utils import decode_predictions
from tensorflow.keras.layers import Input
from tensorflow.keras.preprocessing.image import load_img, img_to_array

from zmlpsdk import AssetProcessor
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

tensorflow.config.set_visible_devices([], 'GPU')


class ZviLabelDetectionProcessor(AssetProcessor):
    """
    Performs image classification using Resnet152 and imagenet weights.
    """

    def init(self):
        self.model = Resnet152ImageClassifier()

    def process(self, frame):
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        predictions = self.model.predict(proxy_path)

        analysis = LabelDetectionAnalysis()
        for label in predictions:
            analysis.add_label_and_score(label[1], label[2])

        asset.add_analysis('zvi-label-detection', analysis)


class Resnet152ImageClassifier:
    def __init__(self):
        self.model = resnet_v2.ResNet152V2(
            weights='imagenet', input_tensor=Input(shape=(224, 224, 3)))
        self.model.compile(loss='mse', optimizer='rmsprop')

    def predict(self, path):
        img = load_image(path)
        result = self.model.predict(resnet_v2.preprocess_input(img))
        return decode_predictions(result)[0]


class VGG16ImageClassifier:
    def __init__(self):
        self.model = vgg_16.VGG16(
            weights='imagenet', input_tensor=Input(shape=(224, 224, 3)))
        self.model.compile(loss='mse', optimizer='rmsprop')

    def predict(self, path):
        img = load_image(path)
        result = self.model.predict(vgg_16.preprocess_input(img))
        return decode_predictions(result)[0]


def load_image(path, size=(224, 224)):
    """
    Load the given image and prepare it for use by Tensorflow.

    Args:
        path (str): The path to the file to load.
        size (tuple): A tuple of width, height

    Returns:
        numpy array: an array of bytes for Tensorflow use.
    """
    img = load_img(
        path,
        grayscale=False,
        color_mode='rgb',
        target_size=size,
        interpolation='nearest'
    )

    numpy_image = img_to_array(img)
    return np.expand_dims(numpy_image, axis=0)
