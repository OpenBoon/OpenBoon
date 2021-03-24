"""
Keras specific utilities.
"""
import logging
import os

import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing.image import load_img, img_to_array

logger = logging.getLogger(__name__)


def load_keras_image(path, size=(224, 224)):
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
        interpolation='lanczos',
    )

    numpy_image = img_to_array(img)
    return np.expand_dims(numpy_image, axis=0)


def load_keras_model(model_path):
    """
    Install the given Boon AI model into the local model cache and return
    the Keras model instance with its array of labels.

    Args:
        model_path (str): A keras model path.
    Returns:
        tuple: (Keras model instance, List[str] of labels)
    """
    # load dir as a model using keras
    trained_model = load_model(model_path)

    # labels.txt is always the name
    # create a list of labels from file labels.txt
    try:
        with open(os.path.join(model_path, 'labels.txt')) as fp:
            labels = fp.read().splitlines()
    except FileNotFoundError:
        logger.warning('failed to find labels.txt file for model')
        labels = []

    # return model and labels
    return trained_model, labels
