import logging
import sys
import os
import shutil
import urllib
from unittest.mock import patch

from zmlp.app import DataSetApp
from zmlp.entity.dataset import DataSet
from zmlp_train.tf2 import TensorflowTransferLearningTrainer
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset

logging.basicConfig(stream=sys.stdout, level=logging.INFO)

basepath = os.path.dirname(__file__)
path_yolo = os.path.abspath(os.path.join(basepath, ".."))

example_img = "dog.jpg"

weights_filepath = f"{path_yolo}/data/yolov3.weights"
weights_basename = os.path.basename(weights_filepath)
weights_url = f"https://pjreddie.com/media/files/{weights_basename}"


# Download the file from `url` and save it locally under `file_name`:
def load_weights():
    if not os.path.exists(weights_filepath):
        logging.info("Downloading weights...")
        with urllib.request.urlopen(weights_url) as response, open(
            weights_filepath, "wb"
        ) as out_file:
            shutil.copyfileobj(response, out_file)
    logging.info("Weights loaded.")


load_weights()
stream = os.popen(
    f"python3 {path_yolo}/yolo.py "
    f"-w {weights_filepath} "
    f"-i {path_yolo}/data/{example_img}"
)
output = stream.read()
logging.info(output)

assert os.path.exists(f"{path_yolo}/data/dog_detected.jpg")
