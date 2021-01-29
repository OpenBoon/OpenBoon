"""
Pytorch specific utilities.
"""
import logging
import os

from zmlpsdk import file_storage

from PIL import Image
import torch
from torchvision import transforms


logger = logging.getLogger(__name__)


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


def load_pytorch_model(model):
    """
    Install the given ZMLP model into the local model cache and return
    the Keras model instance with its array of labels.

    Args:
        model (Model): A zmlp model instance.

    Returns:
        tuple: (Keras model instance, List[str] of labels)
    """
    model_path = file_storage.models.install_model(model)

    trained_model = torch.load(model_path + '/model.pth')
    trained_model.eval()

    try:
        with open(os.path.join(model_path, "labels.txt")) as fp:
            labels = fp.read().splitlines()
    except FileNotFoundError:
        logger.warning("failed to find labels.txt file for model {}".format(model))
        labels = []

    # return model and labels
    return trained_model, labels
