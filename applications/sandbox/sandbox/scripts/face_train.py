import zmlp
from zmlp import app_from_env
from zmlp import DataSetType, DataSet

import numpy as np
import cv2
from PIL import Image
import streamlit as st
import pickle
import os.path
from sklearn.neighbors import KNeighborsClassifier
from zvi.proxies import download_proxy

spread_attrs = ['source.filename', 'media.width', 'media.height']


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


def num_hashes(detections):
    # Take a list of detections, return a numpy array with the hashes
    data = []
    labels = []
    i = 0
    for f in detections:
        num_hash = []
        hash = f['simhash']
        for char in hash:
            num_hash.append(ord(char))
        data.append(num_hash)
        labels.append(f['label'])
        i += 1

    x = np.asarray((data), dtype=np.float64)
    y = np.asarray(labels)

    return x, y


def make_classifier(face_model):

    x_train, y_train = num_hashes(face_model)

    classifier = KNeighborsClassifier(n_neighbors=1, p=1, weights='uniform', metric='manhattan')
    classifier.fit(x_train, y_train)

    return classifier


app = app_from_env()

query = {"sort": "source.filename", "size": 10000, "query": {"exists": {"field": "analysis.zvi-face-detection.type"}}}
search = app.assets.search(query)
count = len(search.assets)

face_model = []
face_classifier = None

try:
    dataset = app.datasets.create_dataset('face_recognition', DataSetType.FACE_RECOGNITION)
except:
    dataset = app.datasets.find_one_dataset(name='face_recognition')


if os.path.isfile('face_recognition.pickle'):
    with open('face_recognition.pickle', 'rb') as input:
        face_model = pickle.load(input)

if face_model:
    face_classifier = make_classifier(face_model)
else:
    face_classifier = None

if st.sidebar.button('Train'):
    dataset = app.datasets.find_one_dataset(name='face_recognition')
    try:
        model = app.models.find_one_model(dataset=dataset)
    except:
        model = app.models.create_model(dataset, zmlp.ModelType.FACE_RECOGNITION_KNN)
    app.models.train_model(model)

if st.sidebar.button('Deploy'):
    model = app.models.find_one_model(dataset=dataset)
    if model:
        module = app.models.publish_model(model)
        search = app.assets.search(query)
        app.assets.reprocess_assets(search.assets, modules=[module.name])


image_index = st.slider('Image', 0, count-1, 0)
#max_dist = st.sidebar.slider('Max Distance', 100, 2000, 1000)
max_dist = 1000
draw_boxes = True
#conf_thresh = st.sidebar.slider('Min Confidence', 0., 1., .01)
conf_thresh = .01

min_conf = 0
max_conf = 1

asset = search.assets[image_index]

img = download_proxy(asset, 1)
img_draw = img.copy()

detections = asset.document['analysis']['zvi-face-detection']
yr = img.shape[0]
xr = img.shape[1]

# Classify existing faces, if we have a classifier
if face_classifier:
    X, _ = num_hashes(detections['predictions'])
    predictions = face_classifier.predict(X)
    dist, ind = face_classifier.kneighbors(X, n_neighbors=1, return_distance=True)
    conf = 1 - np.concatenate(dist) / np.max(np.concatenate(dist))
else:
    predictions = None

st.sidebar.markdown('---')

for i, pred in enumerate(detections['predictions']):
    if predictions is not None:
        pred['label'] = predictions[i]
    bbox = pred['bbox']
    crop = crop_image_poly(img, bbox)
    crop_name = 'tmp/detection' + str(i) + '.jpg'
    cv2.imwrite(crop_name, crop)

    st.sidebar.image(crop)
    st.sidebar.text(pred['score'])
    prev_label = pred['label'] + '?'

    label = st.sidebar.text_input('Name', value=pred['label'] + '?', key=asset.id + str(i))
    if label != prev_label:
        pred['label'] = label
        face_model.append(pred)
        ds_label = dataset.make_label(pred['label'], bbox=bbox, simhash=pred['simhash'])
        app.assets.update_labels(asset, add_labels=ds_label)
    st.sidebar.markdown('---')

with open('face_recognition.pickle', 'wb') as output:  # Overwrites any existing file.
    pickle.dump(face_model, output)

if draw_boxes:
    for i, pred in enumerate(detections['predictions']):
        if pred['score'] >= min_conf and pred['score'] <= max_conf:
            x1 = int(pred['bbox'][0] * xr)
            y1 = int(pred['bbox'][1] * yr)
            x2 = int(pred['bbox'][2] * xr)
            y2 = int(pred['bbox'][3] * yr)
            color = (0, 255, 0)
            if pred['score'] < conf_thresh:
                color = (0, 0, 255)

            min_dist = 800
            if predictions is not None:
                conf = 1 - max(0, min(1, (dist[i][0] - min_dist) / (max_dist - min_dist)))
                if dist[i] < max_dist:
                    text = predictions[i] + ' ' + str(conf)[:4]
                else:
                    text = 'Unrecognized'
                    color = (0, 128, 128)
                cv2.putText(img_draw, text, (x1, y2+20), cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

            cv2.rectangle(img_draw, (x1, y1), (x2, y2), color, 2)

st.image(img_draw)

