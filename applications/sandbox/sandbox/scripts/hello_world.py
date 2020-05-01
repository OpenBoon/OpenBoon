from zmlp import app_from_env
import numpy as np
import cv2
from PIL import Image
import streamlit as st
import random


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

query = {"size": 10000}
search = app.assets.search(query)
count = len(search.assets)
st.text(count)
asset = search.assets[random.randint(0, count)]

img = download_proxy(asset, 2)

st.image(img)
st.sidebar.text("Hello World")

