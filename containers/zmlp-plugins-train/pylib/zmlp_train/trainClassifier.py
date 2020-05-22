#!/usr/bin/env python
"""Train a classifier and save the model"""

import os
import sys
import logging
import argparse
import numpy as np
import random
import tempfile

from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Activation
from tensorflow.keras.optimizers import SGD
from tensorflow.keras.utils import to_categorical

import zmlp
from zmlpsdk import file_storage
from zmlp_train.utils.utils import get_labels, extract_model
from zmlp_train.utils.models import upload_model_directory

logging.basicConfig(level=logging.INFO)

PTEST = 0.3
SEED = 42
ORD_VAL = 65
NORMALIZED_VAL = 16.0


def load_data(app, app_model_name, simhash):
    """Load data into train and test formats that Keras likes

    Args:
        app: (ZmlpClient) ZMLP client
        app_model_name: (str) model name based off Model Id
        simhash: (str) similarity hash attribute

    Returns:
        (tuple) of (np.ndarray, np.ndarray, np.ndarray, np.ndarray, List[str])
    """
    train_x, train_y = [], []
    test_x, test_y = [], []

    random.seed(SEED)

    search = app.assets.search()
    for a in search:
        try:
            charhash = a.get_attr(simhash)
            label = a.get_attr('label')
        except TypeError:
            continue
        num_hash = [
            (ord(char) - ORD_VAL) / NORMALIZED_VAL for char in charhash
        ]

        if random.random() > PTEST:
            train_x.append(num_hash)
            train_y.append(label)
        else:
            test_x.append(num_hash)
            test_y.append(label)

    # Convert labels to an array of indices
    cats = list(set(train_y))
    train_y = [cats.index(i) for i in train_y]
    test_y = [cats.index(i) for i in test_y]

    # Make everything into numpy arrays
    train_x = np.array(train_x)
    train_y = to_categorical(train_y, len(cats))
    test_x = np.array(test_x)
    test_y = to_categorical(test_y, len(cats))

    return (train_x, train_y), (test_x, test_y), cats


def build_model(n_neurons, train_x, train_y, test_x, test_y, class_names):
    """Build and fit the model

    Args:
        n_neurons: (int) number of neurons in the hidden layer
        train_x: (np.ndarray) training values
        train_y: (np.ndarray) training labels
        test_x: (np.ndarray) test values
        test_y: (np.ndarray) test labels
        class_names: (List[str]) list of class names

    Returns:
        tf.keras model
    """
    # add layers
    model = Sequential()
    model.add(Dense(n_neurons, input_dim=train_x.shape[1], activation="relu"))
    model.add(Dense(len(class_names)))
    model.add(Activation("softmax"))

    # train
    sgd = SGD(lr=0.01)
    model.compile(
        loss="categorical_crossentropy", optimizer=sgd, metrics=["accuracy"]
    )
    model.fit(train_x, train_y, epochs=50, batch_size=128, verbose=1)

    # get scores
    scores = model.evaluate(test_x, test_y)
    logging.info(
        "Results on the test set are %f accuracy and %f loss."
        % (scores[1], scores[0])
    )

    return model


def run(main_args):
    args = parse_args(main_args)

    # get model info
    app = zmlp.app_from_env()
    app_model = app.models.get_model(args.model_id)
    model_dir = tempfile.mkdtemp() + '/' + args.folder

    app_model_name = app_model.name
    model_zip = file_storage.projects.localize_file(app_model.file_id)
    tmpfile_dir = extract_model(model_zip)
    class_names = get_labels(tmpfile_dir, app_model_name, "labels.txt")

    # load data
    logging.info("Looking for categories:")
    (train_x, train_y), (test_x, test_y), class_names = load_data(
        app, app_model_name, args.attr
    )

    model = build_model(
        args.n_neurons, train_x, train_y, test_x, test_y, class_names
    )

    # save model and labels
    model.save(model_dir)
    with open(os.path.join(model_dir, "_labels.txt"), "w") as fp:
        for label in class_names:
            fp.write("{}\n".format(label))

    # zip and publish
    upload_model_directory(model_dir, app_model.file_id)
    app.models.publish_model(app_model)

    logging.info("Model saved in " + model_dir)
    logging.info("Labels are in " + model_dir + "/_labels.txt")
    logging.info("Use these two files with the KerasClassifier processor")

    return model_dir


def parse_args(main_args):
    parser = argparse.ArgumentParser(prog="trainClassifier")
    parser.add_argument("-m", "--model_id", help="The model ID")
    parser.add_argument(
        "-a",
        "--attr",
        default="similarity.resnet",
        help="Attribute to use as features.",
    )
    parser.add_argument(
        "-f",
        "--folder",
        default="",
        help="Folder containing subfolders specifying classes. "
        "This will be the name of the classifier.",
    )
    parser.add_argument(
        "-n",
        "--n_neurons",
        default="100",
        help="Number of neurons in the hidden layer.",
    )
    args = parser.parse_args(main_args)
    return args


if __name__ == "__main__":
    sys.exit(run(sys.argv[1:]))
