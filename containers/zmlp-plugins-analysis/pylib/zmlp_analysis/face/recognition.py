import logging
import pickle
import tempfile

from PIL import Image
import numpy as np
from facenet_pytorch import MTCNN, InceptionResnetV1

import zmlp
from zmlp.asset import Element
from zmlp.util import as_collection
from zmlpsdk import AssetProcessor
from zmlpsdk.proxy import get_proxy_level, store_element_proxy
from zmlpsdk.storage import file_storage

NAMESPACE = 'zmlp.faceRecognition'

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
        self.mtcnn = None
        self.resnet = None

    def init(self):
        self.app = zmlp.app_from_env()
        self.mtcnn = MTCNN(image_size=160, margin=20, keep_all=True)
        self.resnet = InceptionResnetV1(pretrained='vggface2').eval()

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

            image = Image.open(get_proxy_level(asset, 3))

            tags = asset.get_attr('datasets.{}'.format(NAMESPACE))

            img_cropped = self.mtcnn(image)

            if not img_cropped:
                continue

            locations = self.mtcnn.detect(image)[0]
            encodings = self.resnet(img_cropped.unsqueeze(0)).detach().numpy()

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
        self.mtcnn = None
        self.resnet = None
        self.known_faces = []
        self.labels = []

    def init(self):
        self.load_model()
        self.mtcnn = MTCNN(image_size=160, margin=20, keep_all=True)
        self.resnet = InceptionResnetV1(pretrained='vggface2').eval()

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

        img = Image.open(p_path)

        # Get cropped and prewhitened image tensor
        unknown_faces = self.mtcnn(img)

        if not unknown_faces:
            return

        boxes = self.mtcnn.detect(img)

        elements = []

        # Iterate boxes, add elements to the asset.
        for i, unknown_face in enumerate(unknown_faces):

            encoding = self.resnet(unknown_faces.unsqueeze(0)[i]).detach().numpy()
            box = boxes[0][i]
            confidence = boxes[1][i]

            results = self.compare_faces(self.known_faces, encoding)

            labels = []
            for i, x in enumerate(results):
                if not x:
                    continue
                labels.extend(self.labels[i])

            # Get rid of any repetitions--if a face is in the model more than once, we might get
            # the same label more than once
            labels = list(set(labels))

            vector = self.generate_hash(encoding)
            element = Element('face',
                              analysis=self.namespace,
                              labels=labels or None,
                              # The rect is not the same format as other libs
                              # Cant' we all just get along?
                              rect=[box[1], box[2], box[3], box[0]],
                              confidence=confidence,
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

        asset.add_analysis(self.namespace, {"faceCount": len(rects)})

    def generate_hash(self, encoding):
        features = 100 * (encoding + .1)
        fh = np.clip(features.astype(int), 0, 24) + 65

        return ''.join([chr(item) for item in fh])

