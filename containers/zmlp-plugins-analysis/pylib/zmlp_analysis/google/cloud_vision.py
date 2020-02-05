import io
import backoff

from pathlib2 import Path

from google.cloud import vision
from google.cloud.vision import types
from google.api_core.exceptions import ResourceExhausted

from zmlpsdk import Argument, ZmlpFatalProcessorException, AssetProcessor
from zmlpsdk.proxy import get_proxy_level

from .gcp_client import initialize_gcp_client


class CloudVisionProcessor(AssetProcessor):
    """Use Google Cloud Vision API to analyze images."""

    tool_tips = {
        'detect_image_text': 'If True, Text Detection performs Optical Character Recognition.'
                             'It detects and extracts text within an image with support for a'
                             'broad range of languages. It also features automatic language'
                             'identification.',
        'detect_document_text': 'If True, Document Text Detection performs Optical Character'
                                'Recognition. This feature detects dense document text -'
                                'including handwriting - in an image.',
        'detect_landmarks': 'If True, Landmark Detection detects popular natural and man-made'
                            'structures within an image.',
        'detect_explicit': 'If True, Safe Search Detection detects explicit content such as'
                           'adult content or violent content within an image. This feature'
                           'uses five categories ("adult", "spoof", "medical", "violence",'
                           'and "racy") and returns the likelihood that each is present in'
                           'a given image',
        'detect_faces': 'If True Face Detection detects multiple faces within an image along'
                        'with the associated key facial attributes such as emotional state'
                        'or wearing headwear.',
        'detect_labels': 'If True Label Detection detects broad sets of categories within an'
                         'image, which range from modes of transportation to animals.',
        'detect_web_entities': 'If True, Web Detection detects Web references to an image.',
        'detect_logos': 'If True, Logo Detection detects popular product logos within an image.',
        'detect_objects': 'If True, Object Localization can detect and extract multiple objects'
                          'in an image.',
    }

    def __init__(self):
        super(CloudVisionProcessor, self).__init__()
        self.add_arg(Argument('detect_image_text', 'bool', default=False,
                              toolTip=self.tool_tips['detect_image_text']))
        self.add_arg(Argument('detect_document_text', 'bool', default=False,
                              toolTip=self.tool_tips['detect_document_text']))
        self.add_arg(Argument('detect_landmarks', 'bool', default=False,
                              toolTip=self.tool_tips['detect_landmarks']))
        self.add_arg(Argument('detect_explicit', 'bool', default=False,
                              toolTip=self.tool_tips['detect_explicit']))
        self.add_arg(Argument('detect_faces', 'bool', default=False,
                              toolTip=self.tool_tips['detect_faces']))
        self.add_arg(Argument('detect_labels', 'bool', default=False,
                              toolTip=self.tool_tips['detect_labels']))
        self.add_arg(Argument('detect_web_entities', 'bool', default=False,
                              toolTip=self.tool_tips['detect_web_entities']))
        self.add_arg(Argument('detect_logos', 'bool', default=False,
                              toolTip=self.tool_tips['detect_logos']))
        self.add_arg(Argument('detect_objects', 'bool', default=False,
                              toolTip=self.tool_tips['detect_objects']))
        self.add_arg(Argument('debug', 'bool', default=False))
        self.image_annotator = None

    def init(self):
        super(CloudVisionProcessor, self).init()
        self.image_annotator = initialize_gcp_client(vision.ImageAnnotatorClient)

    def process(self, frame):
        asset = frame.asset
        path = get_proxy_level(asset, 1)
        if not path:
            return
        if Path(path).stat().st_size > 10485760:
            raise ZmlpFatalProcessorException(
                'The image is too large to submit to Google ML. Image size must '
                'be < 10485760 bytes')
        with io.open(path, 'rb') as image_file:
            content = image_file.read()
        image = types.Image(content=content)
        if self.arg_value('detect_image_text'):
            self._detect_image_text(asset, image)
        if self.arg_value('detect_document_text'):
            self._detect_document_text(asset, image)
        if self.arg_value('detect_landmarks'):
            self._detect_landmarks(asset, image)
        if self.arg_value('detect_explicit'):
            self._detect_explicit(asset, image)
        if self.arg_value('detect_faces'):
            self._detect_faces(asset, image)
        if self.arg_value('detect_labels'):
            self._detect_labels(asset, image)
        if self.arg_value('detect_web_entities'):
            self._detect_web_entities(asset, image)
        if self.arg_value('detect_logos'):
            self._detect_logos(asset, image)
        if self.arg_value('detect_objects'):
            self._detect_objects(asset, image)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_image_text(self, asset, image):
        """Executes text detection on images using the Cloud Vision API."""
        response = self.image_annotator.text_detection(image=image)
        text = response.full_text_annotation.text
        if text:
            if len(text) > 32766:
                text = text[:32765]
            asset.add_analysis('google.imageTextDetection', {'content': text})

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_document_text(self, asset, image):
        """Executes text detection using the Cloud Vision API."""
        response = self.image_annotator.document_text_detection(image=image)
        text = response.full_text_annotation.text
        if text:
            if len(text) > 32766:
                text = text[:32765]
            asset.add_analysis('google.documentTextDetection', {'content': text})

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_landmarks(self, asset, image):
        """Executes landmark detection using the Cloud Vision API."""
        response = self.image_annotator.landmark_detection(image=image)
        landmarks = response.landmark_annotations
        if landmarks:
            # We'll only add the first landmark found for now.
            struct = {}
            landmark = landmarks[0]
            struct['keywords'] = landmark.description
            struct['point'] = (landmark.locations[0].lat_lng.longitude,
                               landmark.locations[0].lat_lng.latitude)
            struct['score'] = float(landmark.score)
            self.logger.info('Storing: {}'.format(struct))
            asset.add_analysis("google.landmarkDetection", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_explicit(self, asset, image):
        """Executes safe-search detection using the Cloud Vision API."""
        response = self.image_annotator.safe_search_detection(image=image)
        safe = response.safe_search_annotation
        struct = {}
        for category in ['adult', 'spoof', 'medical', 'violence', 'racy']:
            rating = getattr(safe, category)
            if rating < 1 or rating > 5:
                continue
            score = (float(rating) - 1.0) * 0.25
            struct[category] = score
        if struct:
            asset.add_analysis("google.explicit", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_faces(self, asset, image):
        """Executes face detection using the Cloud Vision API."""
        response = self.image_annotator.face_detection(image=image)
        faces = response.face_annotations
        if faces:

            # We'll only add the first face found for now. TODO: deal with multiple faces
            face = faces[0]
            struct = {}
            keywords = []
            struct['roll_angle'] = face.roll_angle
            struct['pan_angle'] = face.pan_angle
            struct['tilt_angle'] = face.tilt_angle
            struct['detection_confidence'] = face.detection_confidence
            struct['joy_likelihood'] = face.joy_likelihood
            struct['sorrow_likelihood'] = face.sorrow_likelihood
            struct['anger_likelihood'] = face.anger_likelihood
            struct['surprise_likelihood'] = face.surprise_likelihood
            struct['under_exposed_likelihood'] = face.under_exposed_likelihood
            struct['blurred_likelihood'] = face.blurred_likelihood
            struct['headwear_likelihood'] = face.headwear_likelihood

            # For entries that are likely or very likely, add the attribute name as a keyword.
            for key, value in struct.items():
                if 'likelihood' in key and value >= 4:
                    keywords.append(key.replace('_likelihood', ''))

            # This deduplication is unnecessary now but will be necessary when multiple
            # faces are accounted for
            struct["keywords"] = list(set(keywords))

            asset.add_analysis("google.faceDetection", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_labels(self, asset, image):
        """Executes label detection using the Cloud Vision API."""
        response = self.image_annotator.label_detection(image=image)
        labels = response.label_annotations
        struct = {}
        keywords = []
        for i, label in enumerate(labels):
            keywords.append(label.description)
            if self.arg_value("debug"):
                struct["pred" + str(i)] = label.description
                struct["prob" + str(i)] = labels[i].score
        struct["keywords"] = list(set(keywords))
        if self.arg_value("debug"):
            struct["type"] = "GCLabelDetection"
            struct["scores"] = labels[0].score
        asset.add_analysis("google.labelDetection", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_web_entities(self, asset, image):
        """Executes web entity detection using the Cloud Vision API.
            Image size limit of 10mb
            Args:
                asset(Asset): Frame asset
                image(Image): Image content
        """
        response = self.image_annotator.web_detection(image=image)
        annotations = response.web_detection
        struct = {}
        keywords = []
        for i, entity in enumerate(annotations.web_entities):
            # skipping null values prevent images from being omitted from zvi
            if entity.description:
                keywords.append(entity.description)
                if self.arg_value("debug"):
                    struct["pred" + str(i)] = entity.description
                    struct["prob" + str(i)] = entity.score
        struct["keywords"] = list(set(keywords))
        asset.add_analysis("google.webEntityDetection", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_logos(self, asset, image):
        """Executes logo detection using the Cloud Vision API.
            Image size limit of 10mb
            Args:
                asset(Asset): Frame asset
                image(Image): Image content
        """
        response = self.image_annotator.logo_detection(image=image)
        logos = response.logo_annotations
        struct = {}
        keywords = []
        for i, logo in enumerate(logos):
            # skipping null values prevent images from being omitted from zvi
            if logo.description:
                keywords.append(logo.description)
                if self.arg_value("debug"):
                    struct["pred" + str(i)] = logo.description
                    struct["prob" + str(i)] = logo.score
        struct["keywords"] = list(set(keywords))
        asset.add_analysis("google.logoDetection", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_objects(self, asset, image):
        """Executes logo detection using the Cloud Vision API.
            Image size limit of 10mb
            Args:
                asset(Asset): Frame asset
                image(Image): Image content
        """
        response = self.image_annotator.object_localization(image=image)
        objects = response.localized_object_annotations
        struct = {}
        keywords = []
        for i, object in enumerate(objects):
            # skipping null values prevent images from being omitted from zvi
            print(object)
            if object.name:
                keywords.append(object.name)
                if self.arg_value("debug"):
                    struct["pred" + str(i)] = object.name
                    struct["prob" + str(i)] = object.score
        struct["keywords"] = list(set(keywords))
        asset.add_analysis("google.objectDetection", struct)
