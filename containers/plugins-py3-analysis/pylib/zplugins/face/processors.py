from __future__ import absolute_import

from glob import glob

import cv2
import face_recognition
import numpy as np

from zorroa.zsdk.processor import DocumentProcessor, Argument


class FaceRecognitionProcessor(DocumentProcessor):
    """Detect and recognize faces.
    Generate a face similarity hash
    """
    toolTips = {
        'known_faces_path': 'Path to directory of known faces.'
    }

    def __init__(self):
        super(FaceRecognitionProcessor, self).__init__()
        self.add_arg(Argument('known_faces_path', 'str',
                              self.toolTips['known_faces_path']))

    def init(self):
        self.known_encodings = []
        self.known_names = []

        if self.arg_value('known_faces_path'):
            glob_pattern = self.arg_value('known_faces_path').strip('/') + '/*'
            for f in glob(glob_pattern):
                name = f.split('/')[-1].split('.')[0]
                self.logger.info(name)
                image = face_recognition.load_image_file(f)
                self.known_names.append(name)
                self.known_encodings.append(face_recognition.face_encodings(image)[0])

    def _process(self, frame):
        asset = frame.asset

        p_path = asset.get_thumbnail_path()
        img = cv2.imread(p_path)
        height, width = img.shape[:2]

        # Downsize image if bigger than 1024x1024 in order to avoid OOM errors from face_recognition
        max_width = 1024
        if max(width, height) > max_width:
            scaling_factor = max_width / float(height)
            if max_width / float(width) < scaling_factor:
                scaling_factor = max_width / float(width)
            img = cv2.resize(img, None, fx=scaling_factor, fy=scaling_factor, interpolation=cv2.INTER_AREA)
            width *= scaling_factor
            height *= scaling_factor

        face_locations = face_recognition.face_locations(img, model="cnn")

        encodings = face_recognition.face_encodings(img, known_face_locations=face_locations)

        names = []
        face_attrs = {}

        # Generate face hash
        if len(encodings) > 0:
            fh = np.clip(((np.squeeze(encodings[0]) + 1)*16).astype(int), 0, 32) + 65
            fhash = "".join([chr(item) for item in fh])
            face_attrs["shash"] = fhash

            boxes = []
            for b in face_locations:
                self.logger.info(b)

                ymin = '{:7.5f}'.format(b[0] / float(height))
                xmax = '{:7.5f}'.format(b[1] / float(width))
                ymax = '{:7.5f}'.format(b[2] / float(height))
                xmin = '{:7.5f}'.format(b[3] / float(width))

                print (b)
                boxes.append(((xmin, ymin), (xmax, ymax)))

            kw = 'face' + str(len(face_locations))
            face_attrs["keywords"] = kw
            face_attrs["boxes"] = boxes
            face_attrs["number"] = len(face_locations)

        # Recognize names
        for i in range(0, len(face_locations)):
            results = face_recognition.compare_faces(self.known_encodings, encodings[i])

            if True in results:
                names.append(self.known_names[[i for i, x in enumerate(results)
                                               if x][0]].split('+')[0])

        if names:
            face_attrs["names"] = names

        if not face_attrs:
            return
        asset.add_analysis("faceRecognition", face_attrs)
