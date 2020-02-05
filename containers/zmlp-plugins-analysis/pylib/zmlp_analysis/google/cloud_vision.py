import io
import backoff

from pathlib2 import Path

from google.cloud import vision
from google.cloud.vision import types
from google.api_core.exceptions import ResourceExhausted

from zmlpsdk import Argument, ZmlpFatalProcessorException, AssetProcessor
from zmlpsdk.proxy import get_proxy_level

from .gcp_client import initialize_gcp_client

__all__ = [
    'CloudVisionDetectImageText',
    'CloudVisionDetectDocumentText',
    'CloudVisionDetectLandmarks',
    'CloudVisionDetectExplicit',
    'CloudVisionDetectLabels',
    'CloudVisionDetectFaces',
    'CloudVisionDetectWebEntities',
    'CloudVisionDetectLogos',
    'CloudVisionDetectObjects'
]


class AbstractCloudVisionProcessor(AssetProcessor):
    def __init__(self):
        super(AbstractCloudVisionProcessor, self).__init__()
        self.image_annotator = None
        self.add_arg(Argument('debug', 'bool', default=False))

    def init(self):
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
        self.detect(asset, image)

    def detect(self, asset, image):
        pass


class CloudVisionDetectImageText(AbstractCloudVisionProcessor):

    def __init__(self):
        super(CloudVisionDetectImageText, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
        """Executes text detection on images using the Cloud Vision API."""
        response = self.image_annotator.text_detection(image=image)
        text = response.full_text_annotation.text
        if text:
            if len(text) > 32766:
                text = text[:32765]
            asset.add_analysis('google.imageTextDetection', {'content': text})


class CloudVisionDetectDocumentText(AbstractCloudVisionProcessor):

    def __init__(self):
        super(CloudVisionDetectDocumentText, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
        """Executes text detection using the Cloud Vision API."""
        response = self.image_annotator.document_text_detection(image=image)
        text = response.full_text_annotation.text
        if text:
            if len(text) > 32766:
                text = text[:32765]
            asset.add_analysis('google.documentTextDetection', {'content': text})


class CloudVisionDetectLandmarks(AbstractCloudVisionProcessor):

    def __init__(self):
        super(CloudVisionDetectLandmarks, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
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


class CloudVisionDetectExplicit(AbstractCloudVisionProcessor):

    def __init__(self):
        super(CloudVisionDetectExplicit, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
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


class CloudVisionDetectFaces(AbstractCloudVisionProcessor):

    def __init__(self):
        super(CloudVisionDetectFaces, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
        """Executes face detection using the Cloud Vision API."""
        response = self.image_annotator.face_detection(image=image)
        faces = response.face_annotations
        if faces:

            # We'll only add the first face found for now. TODO: deal with multiple faces
            face = faces[0]
            struct = {}
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

            asset.add_analysis("google.faceDetection", struct)


class CloudVisionDetectLabels(AbstractCloudVisionProcessor):

    def __init__(self):
        super(CloudVisionDetectLabels, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
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


class CloudVisionDetectWebEntities(AbstractCloudVisionProcessor):

    def __init__(self):
        super(CloudVisionDetectWebEntities, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
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


class CloudVisionDetectLogos(AbstractCloudVisionProcessor):

    def __init__(self):
        super(CloudVisionDetectLogos, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
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


class CloudVisionDetectObjects(AbstractCloudVisionProcessor):

    def __init__(self):
        super(CloudVisionDetectObjects, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
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
            if object.name:
                keywords.append(object.name)
                if self.arg_value("debug"):
                    struct["pred" + str(i)] = object.name
                    struct["prob" + str(i)] = object.score
        struct["keywords"] = list(set(keywords))
        asset.add_analysis("google.objectDetection", struct)
