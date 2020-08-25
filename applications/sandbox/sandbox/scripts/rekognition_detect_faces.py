import os
import cv2
import numpy as np
from PIL import Image

import boto3
import streamlit as st

from zmlp import app_from_env
from zmlpsdk.proxy import get_proxy_level_path


class Rekognition:
    """Get labels for an image using AWS Rekognition """

    def __init__(self, max_labels=3):
        os.environ['AWS_ACCESS_KEY_ID'] = "AKIAXAKJCGYIACN6NXPF"
        os.environ['AWS_SECRET_ACCESS_KEY'] = "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"
        os.environ['AWS_DEFAULT_REGION'] = "us-east-2"

        # AWS client
        self.client = boto3.client(
            'rekognition',
            aws_access_key_id=os.environ['AWS_ACCESS_KEY_ID'],
            aws_secret_access_key=os.environ['AWS_SECRET_ACCESS_KEY']
        )

        self.max_labels = max_labels
        self.response = None

    def predict(self, path):
        """ Make a prediction for an image path.

        Args:
            path (str): image path

        Returns:
            (dict): Detected labels
        """
        with open(path, 'rb') as f:
            source_bytes = f.read()

        # get predictions
        img_json = {'Bytes': source_bytes}
        self.response = self.client.detect_faces(
            Image=img_json,
            # MaxLabels=self.max_labels
        )

        return self.response

    def draw_boxes(self, min_conf=0.01, max_conf=1.00, xr=0, yr=0, image=None):
        """Draw bounding boxes for detected labels

        Args:
            min_conf: (float) minimum confidence
            max_conf: (float) maximum confidence
            xr: image width
            yr: image height
            image: (nd.array) image

        Returns:
            None
        """
        def get_location(pixel_value=0.00, img_shape=0):
            """Normalize pixels to create bounding boxes

            Args:
                pixel_value: bounding box value for a detected label
                img_shape: width or height of image

            Returns:
                (int) normalized pixel for bounding box point
            """
            return int(pixel_value * img_shape)

        # iterate through detected labels
        for i, r in enumerate(self.response['FaceDetails']):
            # normalize it's confidence value
            normalized_conf = r['Confidence'] / 100
            # if normalized score is within min/max range
            if min_conf <= normalized_conf <= max_conf:
                bbox = r['BoundingBox']

                left = get_location(bbox['Left'], xr)
                top = get_location(bbox['Top'], yr)
                width = get_location(bbox['Width'], xr)
                height = get_location(bbox['Height'], yr)

                # draw rectangle
                pt1 = (left, top)
                pt2 = (left + width, top + height)
                cv2.rectangle(image, pt1, pt2, (255, 0, 0), 2)

                # add label identifier to rectangle
                org = (left + width, top + height)
                text_font = cv2.FONT_HERSHEY_SIMPLEX
                cv2.putText(image, f"face{i}", org, text_font, 0.7, (255, 0, 0), 2)


def main():
    """Main function """
    app = app_from_env()

    # query
    query = {
        "size": 2000,
        "query": {
            "exists": {
                "field": "analysis.zvi-object-detection.type"
            }
        }
    }
    search = app.assets.search(query)
    count = len(search.assets)

    # set up sidebar
    image_index = st.slider('Image', 0, count - 1, 1)
    draw_boxes = st.sidebar.checkbox('Draw Boxes')
    min_conf = st.sidebar.slider('Min Confidence', 0., 1., .01)
    max_conf = st.sidebar.slider('Max Confidence', 0., 1., 1.)

    # get asset
    asset = search.assets[image_index]

    # get image information
    img = get_proxy_level_path(asset, 2)
    image = np.array(Image.open(img))

    rekognition = Rekognition(max_labels=16)
    response = rekognition.predict(img)

    try:
        yr, xr, _ = image.shape
    except ValueError:
        yr, xr = image.shape

    if draw_boxes:
        rekognition.draw_boxes(min_conf=min_conf, max_conf=max_conf, xr=xr, yr=yr, image=image)

    # show image
    st.image(image)
    # show image dimensions
    st.sidebar.text("Image Shape: {} x {}".format(xr, yr))
    # show JSON response
    st.sidebar.text("Results:")
    st.sidebar.json(response)


if __name__ == '__main__':
    main()
