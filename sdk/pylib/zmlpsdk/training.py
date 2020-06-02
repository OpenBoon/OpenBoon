import logging
import subprocess
import zipfile
import tempfile
import os
import string
import random

import numpy as np
from tensorflow.keras.preprocessing.image import load_img, img_to_array

logger = logging.getLogger(__name__)


def download_dataset(ds_id, style, dst_dir, ratio):
    """
    Download the dataset locally.  Shells out to an external tool
    which handles the downloads in parallel.

    Args:
        ds_id (str): The ID of the dataset.
        style (str): The format the DS should be written into.
        dst_dir (str): The directory to write the DataSet into.
        ratio: (int): The test/train ratio.
    """
    cmd = ['dataset-dl.py',
           ds_id,
           style,
           dst_dir,
           '--train-test-ratio',
           str(ratio)]

    logger.info('Running Cmd: {}'.format(cmd))
    subprocess.call(cmd, shell=False)


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


def id_generator(size=6, chars=string.ascii_uppercase):
    """Generate a random simhash

    Args:
        size: (int) size of hash
        chars: (str) values to use for the hash

    Returns:
        (str) generated similarity hash
    """
    return "".join(random.choice(chars) for _ in range(size))