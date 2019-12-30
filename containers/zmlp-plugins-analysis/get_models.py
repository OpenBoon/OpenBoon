#!/usr/bin/env python3

from tensorflow.keras.applications import ResNet152V2
from tensorflow.keras.applications import ResNet50V2
from tensorflow.keras.applications import DenseNet201

import cvlib
import cv2

print("Downloading YOLO models")
im = cv2.imread("/tmp/fruit.jpg")
cvlib.detect_common_objects(im)
cvlib.detect_common_objects(im, model="yolov3-tiny")

ResNet152V2(weights='imagenet')
ResNet50V2(weights='imagenet')
DenseNet201(weights='imagenet')

