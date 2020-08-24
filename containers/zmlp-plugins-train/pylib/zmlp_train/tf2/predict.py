"""
Keras specific utilities.
"""
import sys
import logging
import os
import argparse

import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing.image import load_img, img_to_array
from tensorflow.keras.applications.resnet_v2 import preprocess_input

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
        color_mode="rgb",
        target_size=size,
        interpolation="lanczos",
    )

    numpy_image = img_to_array(img)
    return np.expand_dims(numpy_image, axis=0)


def load_trained_model():
    """ Load the trained model and labels

    Returns:
        (tuple) Keras trained model and list of labels
    """
    model_path = os.path.dirname(os.path.realpath(__file__))
    trained_model = load_model(model_path)

    # labels.txt is always the name
    # create a list of labels from file labels.txt
    try:
        with open(os.path.join(model_path, "labels.txt")) as fp:
            labels = fp.read().splitlines()
    except FileNotFoundError:
        logger.warning("failed to find labels.txt file")
        labels = []

    # return model and labels
    return trained_model, labels


def predict(path, trained_model, labels):
    """ Make a prediction for an image path

    Args:
        path (str): image path
        trained_model (Keras.model): Keras model instance
        labels (List[str]): list of labels

    Returns:
        List[tuple]: result is list of tuples in format [(label, score),
        (label, score)]
    """
    img = load_keras_image(path)
    # get predictions
    proba = trained_model.predict(preprocess_input(img))[0]
    # create list of tuples for labels and prob scores
    result = [*zip(labels, proba)]
    return result


def main(args):
    args = parse_args(args)

    trained_model, labels = load_trained_model()
    predictions = predict(path=args.img_path, trained_model=trained_model, labels=labels)

    for prediction in predictions:
        print("Label: {0}, Prediction: {1}".format(prediction[0], prediction[1]))


def parse_args(main_args):
    parser = argparse.ArgumentParser(
        description="Get model prediction for an image")

    parser.add_argument(
        "img_path",
        required=True,
        type=str,
        help="Image full path",
    )

    args = parser.parse_args(main_args)
    return args


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
