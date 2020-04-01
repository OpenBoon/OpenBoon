import io

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import vision
from google.cloud.vision import types
from pathlib import Path

from zmlp import Element
from zmlpsdk import Argument, AssetProcessor, ZmlpProcessorException
from zmlpsdk.proxy import get_proxy_level_path, get_proxy_level
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
    """
    This base class is used for all Google Vision features.  Subclasses
    only have to implement the "detect(asset, image) method.
    """

    # The max size of the image you can send to google.
    max_image_size = 10485760

    def __init__(self):
        super(AbstractCloudVisionProcessor, self).__init__()
        self.image_annotator = None
        self.add_arg(Argument('debug', 'bool', default=False))

    def init(self):
        self.image_annotator = initialize_gcp_client(vision.ImageAnnotatorClient)

    def process(self, frame):
        asset = frame.asset
        path = get_proxy_level_path(asset, 1)
        if not path:
            return
        self.detect(asset, self.get_vision_image(path))

    def detect(self, asset, image):
        pass

    def get_vision_image(self, path):
        """
        Loads the file path into a Google Vision Image which is
        used for calling various vision services.
        Args:
            path (str): The file path.

        Returns:
            google.cloud.vision.types.Image
        """
        size = Path(path).stat().st_size
        if size > self.max_image_size:
            raise ZmlpProcessorException('The image is too large to submit '
                                         'to Google ML. Image size ({}) '
                                         'must be < 10485760 bytes'.format(size))
        with io.open(path, 'rb') as image_file:
            content = image_file.read()
        return types.Image(content=content)


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
    namespace = "gcp.face-detection"

    label_keys = [
        'joy',
        'sorrow',
        'anger',
        'surprise',
        'under_exposed',
        'blurred',
        'headwear'
    ]

    def __init__(self):
        super(CloudVisionDetectFaces, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
        """Executes face detection using the Cloud Vision API."""
        # Use large proxy for face
        large_proxy_path = get_proxy_level_path(asset, 3)
        large_proxy = get_proxy_level(asset, 3)

        response = self.image_annotator.face_detection(
            image=self.get_vision_image(large_proxy_path))
        faces = response.face_annotations

        if not faces:
            return
        rects = []
        for face in faces:
            rect = face.bounding_poly
            rects.append([rect.vertices[0].x,
                          rect.vertices[0].y,
                          rect.vertices[2].x,
                          rect.vertices[2].y])

        # Once we have a proxy with boxes we can make elements
        pwidth = large_proxy.attrs["width"]
        pheight = large_proxy.attrs["height"]
        for rect, face in zip(rects, faces):
            element = Element('face', self.namespace,
                              rect=Element.calculate_normalized_rect(pwidth, pheight, rect),
                              labels=self.get_face_labels(face),
                              score=face.detection_confidence)
            asset.add_element(element)

        struct = {
            'detected': len(faces)
        }

        asset.add_analysis(self.namespace, struct)

    def get_face_labels(self, face):
        labels = []
        for key in self.label_keys:
            if getattr(face, "{}_likelihood".format(key)) >= 4:
                labels.append(key)
        return labels


class CloudVisionDetectLabels(AbstractCloudVisionProcessor):

    def __init__(self):
        super(CloudVisionDetectLabels, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, image):
        """Executes label detection using the Cloud Vision API."""
        response = self.image_annotator.label_detection(image=image)
        labels = response.label_annotations

        result = []
        for label in labels:
            result.append({"label": label.description, "score": round(float(label.score), 3)})

        asset.add_analysis("gcp.label-detection", {"labels": result})


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

        all_labels = []
        for obj in objects:

            # build the bounding poly which is not a rect.
            poly = []
            for i in range(0, 4):
                poly.append(obj.bounding_poly.normalized_vertices[i].x)
                poly.append(obj.bounding_poly.normalized_vertices[i].y)

            element = Element("object",
                              "gcp.object-detection",
                              labels=[obj.name],
                              rect=poly,
                              score=round(obj.score, 3))
            asset.add_element(element)
            all_labels.append(obj.name)

        asset.add_analysis("gcp.object-detection", {
            "detected": len(objects),
            "labels": all_labels
        })
