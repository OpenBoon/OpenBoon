#!/usr/bin/env python3

import logging
import os
import argparse

import torch
from PIL import Image
from torchvision import transforms

logger = logging.getLogger('predict')


def load_pytorch_image(path, size=(224, 224)):
    """
    Load the given image and prepare it for use by Tensorflow.

    Args:
        path (str): The path to the file to load.
        size (tuple): A tuple of width, height

    Returns:
        numpy array: an array of bytes for Tensorflow use.
    """

    transform_test = transforms.Compose([
        transforms.Resize(256),
        transforms.CenterCrop(size),
        transforms.ToTensor(),
        transforms.Normalize((0.485, 0.456, 0.406), (0.229, 0.224, 0.225)),
    ])

    img = Image.open(path).convert('RGB')
    scaled_img = transform_test(img)
    torch_image = scaled_img.unsqueeze(0)

    return torch_image


def load_pytorch_model():
    """
    Install the given Boon AI model into the local model cache and return
    the Keras model instance with its array of labels.

    Returns:
        tuple: (Keras model instance, List[str] of labels)
    """
    cwd = os.path.dirname(os.path.realpath(__file__))
    trained_model = torch.load(os.path.join(cwd, 'model.pth'))
    trained_model.eval()

    try:
        with open(os.path.join(cwd, 'labels.txt')) as fp:
            labels = fp.read().splitlines()
    except FileNotFoundError:
        logger.warning('failed to find labels.txt file for model')
        labels = []

    # return model and labels
    return trained_model, labels


def main():

    parser = argparse.ArgumentParser(prog='predict')
    parser.add_argument('image', help='Path to image')
    args = parser.parse_args()

    trained_model, labels = load_pytorch_model()
    image = load_pytorch_image(args.image)

    outputs = trained_model(image)

    probs = torch.nn.functional.softmax(outputs[0], dim=0)
    proba = [float(x) for x in probs]

    # create list of tuples for labels and prob scores
    result = [*zip(labels, proba)]
    for prediction in result:
        print("Label: {0}, Score: {1}".format(prediction[0], prediction[1]))


if __name__ == '__main__':
    main()
