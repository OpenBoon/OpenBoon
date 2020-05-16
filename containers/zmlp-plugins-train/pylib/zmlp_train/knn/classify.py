import shutil
import zipfile
import pickle

import zmlp
from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.storage import FileStorage
from zmlpsdk.analysis import LabelDetectionAnalysis

from sklearn.neighbors import KNeighborsClassifier
import numpy as np


class KnnFaceRecognitionClassifier(AssetProcessor):
    def __init__(self):
        super(KnnFaceRecognitionClassifier, self).__init__()

        self.add_arg(Argument("model_id", "str", required=True,
                              toolTip="The model Id"))

        self.app = zmlp.app_from_env()

        self.app_model = None
        self.face_classifier = None
        self.labels = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))

        model_base_dir = self.app_model.name
        model_zip = FileStorage().localize_file(self.app_model.file_id)

        self.face_classifier = self._extract_info(model_zip, model_base_dir)

    def process(self, frame):
        asset = frame.asset

        faces = asset.get_attr('analysis.zvi-face-detection.predictions')

        if faces is None:
            return

        x = self.num_hashes(faces)
        predictions = self.face_classifier.predict(x)
        dist, ind = self.face_classifier.kneighbors(x, n_neighbors=1, return_distance=True)

        for i, face in enumerate(faces):
            if dist[i][0] < 1000:
                faces[i]['label'] = predictions[i]
                faces[i]['score'] = 1 - max(0, min(1, (dist[i][0] - 800) / (1100 - 800)))
            else:
                faces[i]['label'] = 'Unrecognized'

        analysis = LabelDetectionAnalysis()
        for face in faces:
            analysis.add_label_and_score(face['label'], face['score'],
                                         bbox=face['bbox'], simhash=face['simhash'])

        asset.add_analysis(self.namespace, analysis)

    @staticmethod
    def _extract_info(model_zip, model_base_dir):
        """Extract then remove model info from a zip file

        Parameters
        ----------
        model_zip: str
            model zip dir
        model_base_dir: str
            model.name which is set as model parent dir

        Returns
        -------
        tuple
            (Keras model instance, List[str] of labels)
        """
        with zipfile.ZipFile(model_zip) as z:
            z.extractall()

        with open(model_base_dir + '/face_classifier.pickle', 'rb') as input:
            face_classifier = pickle.load(input)

        shutil.rmtree(model_base_dir)
        try:
            shutil.rmtree("__MACOSX")
        except:
            pass

        return face_classifier

    @staticmethod
    def num_hashes(detections):
        # Take a list of detections, return a numpy array with the hashes
        data = []
        i = 0
        for f in detections:
            num_hash = []
            hash = f['simhash']
            for char in hash:
                num_hash.append(ord(char))
            data.append(num_hash)
            i += 1

        x = np.asarray(data, dtype=np.float64)

        return x
