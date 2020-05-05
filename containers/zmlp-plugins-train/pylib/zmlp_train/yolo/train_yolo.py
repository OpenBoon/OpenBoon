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
from ..tf2.train import TensorflowTransferLearningTrainer
from ..utils.models import upload_model_directory, download_dataset
from .yolo import make_yolov3_model, WeightReader

warnings.filterwarnings("ignore")


class YOLOTransferLearningTrainer(TensorflowTransferLearningTrainer):
    """ YOLO v3 training """

    def __init__(self):
        super(YOLOTransferLearningTrainer, self).__init__()

        self.add_arg(
            Argument(
                "model_type",
                "str",
                required=False,
                toolTip="The the base model type.",
            )
        )
        self.add_arg(
            Argument(
                "weights",
                "str",
                required=True,
                toolTip="Config weights for YOLOv3 model.",
            )
        )

    def get_base_model(self):
        """
        Choose the base YOLO model for transfer learning

        Returns:
            Model: A YOLOv3 model.

        Raises:
            ZmlpFatalProcessorException: If the model is not found

        """
        try:
            # yolo_model = make_yolov3_model()
            # model = Sequential()
            # model.add(yolo_model.layers[0])
            # return model

            weights_path = self.arg_value("weights")
            yolov3 = make_yolov3_model()
            # load the weights trained on COCO into the model
            weight_reader = WeightReader(weights_path)
            weight_reader.load_weights(yolov3)
            return yolov3
        except:
            raise ZmlpFatalProcessorException("Invalid model")
