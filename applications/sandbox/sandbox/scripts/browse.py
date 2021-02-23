from boonsdk import app_from_env
from boonsdk.search import SimilarityQuery
import numpy as np
import cv2
from PIL import Image
import streamlit as st
import pickle
import json
import pandas as pd

from boonlab.proxies import download_proxy

spread_attrs = ['source.filename', 'media.width', 'media.height']


def similarity_search(h):

    min_score = 0.0

    q = {
        "query": {
            "bool": {
                "must": [
                    SimilarityQuery(h, min_score=min_score)
                ]
            }
        }
    }

    app = app_from_env()
    search = app.assets.search(q)
    return search


app = app_from_env()

search = app.assets.search()
count = len(search.assets)


sim_search = False


PAGESIZE = int(st.sidebar.text_input(label='Page Size', value='15'))


image_index = int(st.sidebar.text_input(label='Image Index', value='0'))

if st.sidebar.button('Similarity Search'):
    sim_search = True

if st.sidebar.button('Reset Search'):
    sim_search = False

if sim_search:
    page = 1
    with open('search.pickle', 'rb') as input:
        prev_search = pickle.load(input)
        if image_index >= len(prev_search.assets):
            image_index = 0
        asset = prev_search.assets[image_index]
        h = asset.document['analysis']['boonai-image-similarity']['simhash']
        ss = similarity_search(h)
        count = len(ss.assets)

else:
    n_pages = count // PAGESIZE + 1
    if n_pages < 2:
        n_pages = 2

    st.text('Found ' + str(count) + ' images.')
    page = st.slider('Page', 1, n_pages+1, 1)

if sim_search:
    if image_index >= len(prev_search.assets):
        image_index = 0
    asset = prev_search.assets[image_index]
    h = asset.document['analysis']['boonai-image-similarity']['simhash']
    search = similarity_search(h)
else:
    search = app.assets.search({"from": (page-1)*PAGESIZE, "size": PAGESIZE})

with open('search.pickle', 'wb') as output:  # Overwrites any existing file.
    pickle.dump(search, output)

paths = []
images = []
for a in search.assets:
    name = 'tmp/' + str(a.id) + '.jpg'
    paths.append(name)
    img = download_proxy(a, 0)
    img = cv2.resize(img, None, fx=.5, fy=.5)
    images.append(img)

caption = list(range(0,len(search.assets)))
try:
    st.image(images, caption=caption)
except:
    pass
st.sidebar.text("Document Contents")
if len(search.assets) <= image_index:
    image_index = 0

st.sidebar.json(search.assets[image_index].document)

data = {}
for attr in spread_attrs:
    col = []
    for a in search.assets:
        val = eval('a.document[\'' + attr.replace('.', '\'][\'') + '\']')
        col.append(val)
    data[attr] = col

df = pd.DataFrame.from_dict(data)
st.table(df)
