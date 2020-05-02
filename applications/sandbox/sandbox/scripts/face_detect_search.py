from zmlp import app_from_env
from zmlp.search import SimilarityQuery
import numpy as np
import cv2
from PIL import Image
import streamlit as st
import pickle
import json
import pandas as pd
import io
import random


spread_attrs = ['source.filename', 'media.width', 'media.height']

def download_proxy(asset, level):

    app = app_from_env()

    proxies = asset.get_files(category="proxy", mimetype="image/", sort_func=lambda f: f.attrs["width"])

    if not proxies:
        return None

    if level >= len(proxies):
        level = -1

    proxy = proxies[level]
    img_data = app.assets.download_file(proxy.id)
    img = np.array(Image.open(img_data))

    if len(img.shape) == 2:
        img = cv2.cvtColor(img,cv2.COLOR_GRAY2RGB)

    return img


def crop_image_poly(image, poly, width=256, color=(255, 0, 0), thickness=3):
    """Function accepts an opencv image and returns a new image cropped to just show the
    box given. The output image is also resized to the given width while maintaining the original aspect ratio.
    Args:
        image (numpy.ndarray): Opencv image.
        poly (list): List of coordinates given as percentages. If len(box)==4, it is assumed to be a bounding box.
                     Otherwise it is a list of vertices. In this case the polygon is drawn and then the image
                     cropped to the polygon's bounding box.
    Returns (numpy.ndarray): Opencv image cropped to show the box.

    """

    yr = image.shape[0]
    xr = image.shape[1]

    poly = [(i > 0) * i for i in poly]

    # If poly is four numbers, assume it's a bounding box. Otherwise it's a polygon
    if len(poly) == 4:
        x1 = int(poly[0] * xr)
        y1 = int(poly[1] * yr)
        x2 = int(poly[2] * xr)
        y2 = int(poly[3] * yr)
        draw = image
    else:
        point_list = []
        for i in range(0, len(poly), 2):
            x = int(poly[i] * xr)
            y = int(poly[i + 1] * yr)
            point_list.append([x, y])

        pts = np.array([point_list], np.int32)
        pts = pts.reshape((-1, 1, 2))
        draw = cv2.polylines(image, [pts], True, color, thickness)

        # Now figure out where to crop, using the bounding box of all those points
        v_min = np.min(pts, axis=0)
        v_max = np.max(pts, axis=0)

        y1 = v_min[0][1] - thickness
        y2 = v_max[0][1]
        x1 = v_min[0][0] - thickness
        x2 = v_max[0][0]

    cropped_image = draw[y1:y2, x1:x2]

    xrc = cropped_image.shape[1]
    scale = width / xrc
    resized = cv2.resize(cropped_image, (0, 0), fx=scale, fy=scale)

    return resized


app = app_from_env()

query = {"size": 10000, "query": {"exists": {"field": "analysis.zvi-face-detection.type"}}}
search = app.assets.search(query)
count = len(search.assets)



saved = [0, 832, 281, 619, 732, 842, 913]


saved_search = st.sidebar.selectbox('Saved Assets', saved)


image_index = st.slider('Image', 0, count-1, saved_search)
draw_boxes = st.sidebar.checkbox('Draw Boxes')
conf_thresh = st.sidebar.slider('Min Confidence', 0., 1., .01)
#max_conf = st.sidebar.slider('Max Confidence', 0., 1., 1.)

min_conf = 0
max_conf = 1

asset = search.assets[image_index]

img = download_proxy(asset, 2)
img_draw = img.copy()


detections = asset.document['analysis']['zvi-face-detection']
yr = img.shape[0]
xr = img.shape[1]


if draw_boxes:
    for pred in detections['predictions']:
        if pred['score'] >= min_conf and pred['score'] <= max_conf:
            x1 = int(pred['bbox'][0] * xr)
            y1 = int(pred['bbox'][1] * yr)
            x2 = int(pred['bbox'][2] * xr)
            y2 = int(pred['bbox'][3] * yr)
            color = (0, 255, 0)
            if pred['score'] < conf_thresh:
                color = (0, 0, 255)
            cv2.rectangle(img_draw, (x1, y1), (x2, y2), color, 2)
            cv2.putText(img_draw, pred['label'], (x1, y1-10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

st.image(img_draw)

st.sidebar.markdown('---')

for i, pred in enumerate(detections['predictions']):
    bbox = pred['bbox']
    crop = crop_image_poly(img, bbox)
    crop_name = 'tmp/detection' + str(i) + '.jpg'
    cv2.imwrite(crop_name, crop)

    st.sidebar.image(crop)
    st.sidebar.text(pred['score'])
    st.sidebar.markdown('---')

