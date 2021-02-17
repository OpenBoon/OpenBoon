#!/usr/bin/env python
"""Train a classifier and save the model"""

import os
import logging
import numpy as np
import random
import tempfile

from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Activation
from tensorflow.keras.optimizers import SGD
from tensorflow.keras.utils import to_categorical

from boonflow import AssetProcessor, Argument, file_storage

logging.basicConfig(level=logging.INFO)


class LabelDetectionPerceptronTrainer(AssetProcessor):
    """Trainer for Label Detection Perceptron """

    # determines the supported FileTypes for a Processor
    # setting to `None` since it is a "fake" Processor that doesn't run on
    # each asset and instead runs only once
    file_types = None

    PTEST = 0.3
    SEED = 42
    ORD_VAL = 65
    NORMALIZED_VAL = 16.0

    def __init__(self):
        super(LabelDetectionPerceptronTrainer, self).__init__()

        self.add_arg(
            Argument("model_id", "str", required=True, toolTip="The model Id")
        )
        self.add_arg(Argument("deploy", "bool", default=False,
                              toolTip="Automatically deploy the model onto assets."))
        self.add_arg(
            Argument("attr", "str",
                     required=True,
                     default="analysis.boonai-image-similarity.simhash",
                     toolTip="Attribute to use as features.")
        )
        self.add_arg(
            Argument("n_neurons", "int",
                     required=True,
                     default=100,
                     toolTip="Total number of neurons in the hidden layer")
        )
        self.add_arg(
            Argument("folder", "str",
                     default="",
                     toolTip="Folder containing subfolders specifying classes."
                             " This will be the name of the classifier.")
        )

        self.app_model = None
        self.attr = None
        self.class_names = []

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.attr = self.arg_value('attr')

    def process(self, frame):
        # load data
        logging.debug("Looking for categories:")
        (train_x, train_y), (test_x, test_y), self.class_names = \
            self.load_data(self.attr)

        # build model
        n_neurons = self.arg_value('n_neurons')
        model = self.build_model(
            n_neurons, train_x, train_y, test_x, test_y, self.class_names
        )

        # publish
        self.publish_model(model)

    def load_data(self, simhash):
        """Load data into train and test formats that Keras likes

        Args:
            simhash: (str) similarity hash attribute

        Returns:
            (tuple) of (np.ndarray, np.ndarray, np.ndarray, np.ndarray, List[str])
        """
        train_x, train_y = [], []
        test_x, test_y = [], []

        random.seed(self.SEED)

        query = {
            '_source': ['labels.*', 'analysis.boonai-image-similarity.*'],
            'size': 25,
            'query': {
                'nested': {
                    'path': 'labels',
                    'query': {
                        'bool': {
                            'must': [
                                {'term': {'labels.modelId': self.app_model.id}},
                                {'term': {'labels.scope': 'TRAIN'}}
                            ]
                        }
                    }
                }
            }
        }

        search = self.app.assets.scroll_search(query)
        for asset in search:
            for labels in asset['labels']:
                if labels['modelId'] == self.app_model.id:
                    charhash = asset.get_attr(simhash)
                    label = labels['label']
                    num_hash = [
                        (ord(char) - self.ORD_VAL) / self.NORMALIZED_VAL for char in charhash
                    ]

                    if random.random() > self.PTEST:
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

    @staticmethod
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
        logging.debug(
            "Results on the test set are %f accuracy and %f loss."
            % (scores[1], scores[0])
        )

        return model

    def publish_model(self, model):
        """Publish the model.

        Args:
            model (LabelDetectionPerceptionClassifier): Perceptron
            Classifier instance

        Returns:
            AnalysisModule: The published Pipeline Module.
        """
        model_dir = os.path.join(tempfile.mkdtemp(), self.arg_value('folder'))

        # save model and labels
        model.save(model_dir)
        with open(os.path.join(model_dir, "_labels.txt"), "w") as fp:
            for label in self.class_names:
                fp.write("{}\n".format(label))

        logging.debug("Model saved in " + model_dir)
        logging.debug("Labels are in " + model_dir + "_labels.txt")
        logging.debug("Use these two files with the PerceptronClassifier processor")

        # publish
        pmod = file_storage.models.save_model(model_dir, self.app_model, self.arg_value('deploy'))
        self.reactor.emit_status(
            "Published model {}".format(self.app_model.name))
        return pmod
