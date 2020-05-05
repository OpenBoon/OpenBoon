import logging
import sys
import os
import shutil
import urllib
from argparse import Namespace

from zmlp_train.yolo import main

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


def test_main():
    args = Namespace(
        weights=weights_filepath,
        image="{}/data/{}".format(path_yolo, example_img),
    )

    load_weights()
    main(args)

    assert os.path.exists(f"{path_yolo}/data/dog_detected.jpg")
    os.remove(f"{path_yolo}/data/dog_detected.jpg")


test_main()
