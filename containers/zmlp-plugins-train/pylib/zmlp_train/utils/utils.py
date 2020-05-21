import zipfile
import tempfile
import os

import numpy as np
from tensorflow.keras.preprocessing.image import load_img, img_to_array


def get_labels(*args):
    """Retrieve labels from labels txt file

    Args:
        *args: (*str) filepath, must be in order
            (e.g. "foo", "bar", "labels.txt" for foo/bar/labels.txt)

    Returns:
        (List[str]) of labels
    """
    with open(os.path.join(*args)) as fp:
        labels = fp.read().splitlines()

    return labels


def extract_model(model_zip):
    """ Extract then remove model info from a zip file

    Args:
        model_zip (str): model zip dir

    Returns:
        (str) tmpfile directory
    """
    loc = tempfile.mkdtemp()

    # extract all files
    with zipfile.ZipFile(model_zip) as z:
        z.extractall(path=loc)

    return loc


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
        color_mode="rgb",
        target_size=size,
        interpolation="nearest",
    )

    numpy_image = img_to_array(img)
    return np.expand_dims(numpy_image, axis=0)
