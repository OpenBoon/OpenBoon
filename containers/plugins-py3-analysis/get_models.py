#!/usr/bin/env python3

from tensorflow.keras.applications import ResNet152V2
from tensorflow.keras.applications import ResNet50V2
from tensorflow.keras.applications import DenseNet201


ResNet152V2(weights='imagenet')

ResNet50V2(weights='imagenet')

DenseNet201(weights='imagenet')
