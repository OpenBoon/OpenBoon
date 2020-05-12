import tempfile
import numpy as np
import tensorflow as tf
from keras.layers import (
    Conv2D,
    Input,
    BatchNormalization,
    LeakyReLU,
    ZeroPadding2D,
    UpSampling2D,
    Dropout,
    Flatten,
    Dense,
)
from keras.layers.merge import add, concatenate
from keras.models import Model, load_model
from tensorflow.keras.models import Sequential
from tensorflow.keras.preprocessing.image import ImageDataGenerator

import logging
import sys
import os
import shutil
import urllib
import struct
import cv2
import warnings

import zmlp
from zmlpsdk import AssetProcessor, Argument, ZmlpFatalProcessorException
from ..utils.models import upload_model_directory, download_dataset

warnings.filterwarnings("ignore")


class YOLOTransferLearningTrainer(AssetProcessor):
    """ YOLO v3 training """

    def __init__(self):
        super(YOLOTransferLearningTrainer, self).__init__()

        # These are the base args
        self.add_arg(Argument("dataset_id", "str", required=True,
                              toolTip="The dataset Id"))
        self.add_arg(Argument("name", "str", required=True,
                              toolTip="The name of the model, which is the pipeline mod name."))
        self.add_arg(Argument("file_id", "str", required=True,
                              toolTip="The file_id where the model should be stored"))
        self.add_arg(Argument("publish", "bool", required=False,
                              toolTip="True if the pipeline module should be created/updated"))
        self.add_arg(Argument("weights", "str", required=True,
                              toolTip="Config weights for YOLOv3 model."))

        # These can be set optionally.
        self.add_arg(Argument("epochs", "int", required=True, default=10,
                              toolTip="The number of training epochs"))
        self.add_arg(Argument("min_concepts", "int", required=True, default=2,
                              toolTip="The min number of concepts needed to train."))
        self.add_arg(Argument("min_examples", "int", required=True, default=10,
                              toolTip="The min number of examples needed to train"))
        self.add_arg(
            Argument("train-test-ratio", "int", required=True, default=3,
                     toolTip="The number of training images vs test images"))

        self.app = zmlp.app_from_env()
        self.ds = None
        self.model = None
        self.labels = None
        self.base_dir = None

    def init(self):
        self.ds = self.app.datasets.get_dataset(self.arg_value('dataset_id'))
        self.labels = self.app.datasets.get_label_counts(self.ds)
        self.base_dir = tempfile.mkdtemp('yolo3-xfer-learning')
        self.check_labels()

    def check_labels(self):
        """
        Check the dataset labels to ensure we have enough labels and example images.

        """
        min_concepts = self.arg_value('min_concepts')
        min_examples = self.arg_value('min_examples')

        # Do some checks here.
        if len(self.labels) < min_concepts:
            raise ValueError(
                'You need at least {} labels to train.'.format(min_concepts))

        for name, count in self.labels.items():
            if count < min_examples:
                msg = 'You need at least {} examples to train, {} has  {}'
                raise ValueError(msg.format(min_examples, name, count))

    def process(self, frame):
        download_dataset(self.ds.id, self.base_dir,
                         self.arg_value('train-test-ratio'))
        self.build_model()

        train_gen, test_gen = self.build_generators()
        self.model.fit_generator(
            train_gen,
            validation_data=test_gen,
            epochs=self.arg_value('epochs')
        )

        # Build the label list
        labels = [None] * len(self.labels)
        for label, idx in train_gen.class_indices.items():
            labels[int(idx)] = label

        # self.publish_model(labels)
