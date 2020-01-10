import logging
import pickle
import tempfile

import cv2
import face_recognition
import numpy as np

import zmlp
from zmlp.analysis import AssetProcessor
from zmlp.analysis.proxy import get_proxy_level, store_element_proxy
from zmlp.analysis.storage import file_storage
from zmlp.elements import Element
from zmlp.util import as_collection

NAMESPACE = 'zmlpFaceRecognition'

BOX_COLOR = (0, 255, 255)

class ZmlpBuildFaceRecognitionModel(AssetProcessor):
    """
    Builds a ZmlpFaceRecognitionModel.

    The model is stored in project storage as:

    /models/zmlp_face_recognition_v1/default.faces

    datasets.zmlpFaceRecognition = [
        {
            "point": [x,y],
            "label": "bob dole"
        },
        {
            "point": [x, y],
            "label": "bilbo baggins"
        }
    ]

    """
    search = {
        'query': {
            'exists': {
                'field': 'datasets.{}.labels'.format(NAMESPACE)
            }
        }
    }

    def __init__(self):
        super(ZmlpBuildFaceRecognitionModel, self).__init__()

    def init(self):
        self.app = zmlp.app_from_env()

    def process(self, frame):
        model = self.build_model()

        with tempfile.NamedTemporaryFile('wb', buffering=0, suffix='.dat') as tf:
            pickle.dump(model, tf)
            file_storage.projects.store_file(tf.name, 'model', NAMESPACE, 'default.faces')

    def build_model(self):
        model = {
            'encodings': [],
            'labels': []
        }

        for asset in self.app.assets.search(self.search):
            # TODO: Handle a rect argument to box in specific faces.
            # Note here, we're grabbing the first face for now.
            image = face_recognition.load_image_file(get_proxy_level(asset, 3))
            tags = asset.get_attr('datasets.{}'.format(NAMESPACE))

            locations = face_recognition.face_locations(image, model='cnn')
            encodings = face_recognition.face_encodings(image)
            if not locations or not encodings:
                continue

            for encoding, loc in zip(encodings, locations):
                for tag in tags:
                    point = tag['point']
                    labels = tag['label']

                    if loc[3] <= point[0] <= loc[1] and loc[0] <= point[1] <= loc[2]:
                        model['encodings'].append(encoding)
                        model['labels'].append(as_collection(labels))

        return model


class ZmlpFaceRecognitionProcessor(AssetProcessor):
    """Detect and recognize faces. """

    namespace = NAMESPACE

    def __init__(self):
        super(ZmlpFaceRecognitionProcessor, self).__init__()
        self.known_faces = []
        self.labels = []

    def init(self):
        self.load_model()

    def load_model(self):
        try:
            path = file_storage.projects.localize_file('model', NAMESPACE, 'default.faces')
            self.logger.info("loading model {} from path {}".format(NAMESPACE, path))

            model = pickle.load(open(path, 'rb'))
            self.known_faces = np.array(model['encodings'])
            self.labels = model['labels']
        except Exception as e:
            self.logger.warn('No face recognition model was loaded')
            if self.logger.isEnabledFor(logging.DEBUG):
                self.logger.exception('Error loading face model: {}'.format(e))

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level(asset, 3)

        img = cv2.imread(p_path)
        face_locations = face_recognition.face_locations(img, model='cnn')
        unknown_faces = face_recognition.face_encodings(img)

        if not unknown_faces:
            return

        elements = []

        # Iterate boxes, add elements to the asset.
        for box, unknown_face in zip(face_locations, unknown_faces):
            results = face_recognition.compare_faces(self.known_faces, unknown_face)

            labels = []
            for i, x in enumerate(results):
                if not x:
                    continue
                labels.extend(self.labels[i])

            vector = self.generate_hash(unknown_face)
            element = Element('face',
                              analysis=self.namespace,
                              labels=labels or None,
                              # The rect is not the same format as other libs
                              # Cant' we all just get along?
                              rect=[box[1], box[2], box[3], box[0]],
                              vector=vector)
            elements.append(element)

        # Before we can make proxy we need a list of labels
        # to go with the rects

        labels = [e.labels for e in elements if e.labels] or None
        rects = [e.rect for e in elements]
        proxy = store_element_proxy(asset,
                                    img,
                                    self.namespace,
                                    rects=rects,
                                    labels=labels,
                                    color=BOX_COLOR)

        for element in elements:
            element.set_proxy(proxy)
            asset.add_element(element)

    def generate_hash(self, encoding):
        fh = np.clip(((np.squeeze(encoding) + 1) * 16).astype(int), 0, 32) + 65
        return ''.join([chr(item) for item in fh])
