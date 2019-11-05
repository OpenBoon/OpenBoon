#!/usr/bin/env python3

from tensorflow.keras.applications import ResNet152V2
ResNet152V2(weights='imagenet')

from tensorflow.keras.applications import ResNet50V2
ResNet50V2(weights='imagenet')

from tensorflow.keras.applications import DenseNet201
DenseNet201(weights='imagenet')
