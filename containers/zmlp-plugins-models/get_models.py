#!/usr/bin/env python3

from tensorflow.keras.applications import ResNet152V2
import cvlib
import cv2

print("Downloading YOLO models")
im = cv2.imread("/tmp/fruit.jpg")
cvlib.detect_common_objects(im)

print("Downloading Tensorflow models")
ResNet152V2(weights='imagenet')
