from boonsdk import app_from_env
import numpy as np
import cv2
from PIL import Image
import streamlit as st
import random
from boonlab.proxies import download_proxy


app = app_from_env()

query = {"size": 10000}
search = app.assets.search(query)
count = len(search.assets)
st.text(count)
asset = search.assets[random.randint(0, count)]

img = download_proxy(asset, 2)

st.image(img)
st.sidebar.text("Hello World")

