from zmlp import app_from_env
from zmlp.search import SimilarityQuery
import numpy as np
import cv2
from PIL import Image
import streamlit as st
import pickle
import json
import pandas as pd


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

app = app_from_env()

query = {"size": 2000, "query": {"exists": {"field": "analysis.zvi-object-detection.type"}}}
search = app.assets.search(query)
count = len(search.assets)

image_index = st.slider('Image', 0, count-1, 1)
draw_boxes = st.sidebar.checkbox('Draw Boxes')
min_conf = st.sidebar.slider('Min Confidence', 0., 1., .01)
max_conf = st.sidebar.slider('Max Confidence', 0., 1., 1.)

asset = search.assets[image_index]

img = download_proxy(asset, 2)

detections = asset.document['analysis']['zvi-object-detection']

yr = img.shape[0]
xr = img.shape[1]

if draw_boxes:
    for pred in detections['predictions']:
        if pred['score'] >= min_conf and pred['score'] <= max_conf:
            x1 = int(pred['bbox'][0] * xr)
            y1 = int(pred['bbox'][1] * yr)
            x2 = int(pred['bbox'][2] * xr)
            y2 = int(pred['bbox'][3] * yr)
            cv2.rectangle(img, (x1, y1), (x2, y2), (255,0,0), 2)
            cv2.putText(img, pred['label'], (x1, y1-10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0,255,0), 2)

st.image(img)
st.sidebar.text(str(xr) + ' x ' + str(yr))
st.sidebar.text("Detections")
st.sidebar.json(asset.document['analysis']['zvi-object-detection'])

