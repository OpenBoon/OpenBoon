import io

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import vision
from google.cloud.vision import types

from zmlpsdk import file_storage, Argument, AssetProcessor
from zmlpsdk.proxy import get_proxy_level, calculate_normalized_bbox
from .gcp_client import initialize_gcp_client

__all__ = [
    'CloudVisionDetectImageText',
    'CloudVisionDetectDocumentText',
    'CloudVisionDetectLandmarks',
    'CloudVisionDetectExplicit',
    'CloudVisionDetectLabels',
    'CloudVisionDetectFaces',
    'CloudVisionDetectLogos',
    'CloudVisionDetectObjects'
]


class AbstractCloudVisionProcessor(AssetProcessor):
    """
    This base class is used for all Google Vision features.  Subclasses
    only have to implement the "detect(asset, image) method.
    """

    def __init__(self):
        super(AbstractCloudVisionProcessor, self).__init__()
        self.image_annotator = None
        self.add_arg(Argument('debug', 'bool', default=False))
        self.proxy_level = 1

    def init(self):
        self.image_annotator = initialize_gcp_client(vision.ImageAnnotatorClient)

    def process(self, frame):
        asset = frame.asset
        proxy = get_proxy_level(asset, self.proxy_level)
        if not proxy:
            return
        self.detect(asset, proxy)

    def detect(self, asset, proxy):
        """
        Implemented by sub-classes to perform the necessary processing..

        Args:
            asset (Asset): The asset to process.
            proxy (StoredFile): The file to use.
        """
        pass

    def get_vision_image(self, proxy):
        """
        Loads the file path into a Google Vision Image which is
        used for calling various vision services.  If the file is
        in gs:// already, then the URI is passed to google.
        Otherwise the file is uploaded.

        Args:
            proxy (StoredFile): The StoredFile instance.

        Returns:
            google.cloud.vision.types.Image

        """
        location = file_storage.assets.get_native_uri(proxy)
        if location.startswith("gs://"):
            image = types.Image()
            image.source.image_uri = location
            return image
        else:
            path = file_storage.localize_file(proxy)
            with io.open(path, 'rb') as fp:
                content = fp.read()
            return types.Image(content=content)


class CloudVisionDetectImageText(AbstractCloudVisionProcessor):
    """Executes Image Text Detection the Cloud Vision API."""

    def __init__(self):
        super(CloudVisionDetectImageText, self).__init__()
        self.image_level = 3

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        """Executes text detection on images using the Cloud Vision API."""
        rsp = self.image_annotator.text_detection(image=self.get_vision_image(proxy))
        result = rsp.full_text_annotation

        text = result.text
        if not text:
            return

        if len(text) > 32766:
            text = text[:32765]

        words = text.split()
        text = " ".join(words)

        asset.add_analysis('gcp-vision-image-text-detection', {
            'content': text,
            'type': 'content',
            'count': len(words)
        })


class CloudVisionDetectDocumentText(AbstractCloudVisionProcessor):
    """Executes Document Text Detection the Cloud Vision API."""
    def __init__(self):
        super(CloudVisionDetectDocumentText, self).__init__()
        self.image_level = 3

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.document_text_detection(image=self.get_vision_image(proxy))
        text = rsp.full_text_annotation.text
        if not text:
            return

        if len(text) > 32766:
            text = text[:32765]

        words = text.split()
        text = " ".join(words)

        asset.add_analysis('gcp-vision-doc-text-detection', {
            'content': text,
            'type': 'content',
            'count': len(words)
        })


class CloudVisionDetectLandmarks(AbstractCloudVisionProcessor):
    """Executes landmark detection using the Cloud Vision API."""

    def __init__(self):
        super(CloudVisionDetectLandmarks, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.landmark_detection(image=self.get_vision_image(proxy))
        landmarks = rsp.landmark_annotations
        if not landmarks:
            return

        predictions = []
        for landmark in landmarks:
            predictions.append({
                'label': landmark.description,
                'score': round(float(landmark.score), 3),
                'point': {
                    'lat': landmark.locations[0].lat_lng.latitude,
                    'lon':  landmark.locations[0].lat_lng.longitude
                }
            })

        asset.add_analysis('gcp-vision-landmark-detection', {
            'type': 'landmarks',
            'predictions': predictions,
            'count': len(predictions)
        })


class CloudVisionDetectExplicit(AbstractCloudVisionProcessor):
    """Executes Safe Search using the Cloud Vision API."""

    def __init__(self):
        super(CloudVisionDetectExplicit, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.safe_search_detection(image=self.get_vision_image(proxy))
        result = rsp.safe_search_annotation

        predictions = []
        safe = True

        for category in ['adult', 'spoof', 'medical', 'violence', 'racy']:
            rating = getattr(result, category)
            if rating <= 1 or rating > 5:
                continue
            score = (float(rating) - 1.0) * 0.25
            if score >= 0.50:
                safe = False

            predictions.append({
                'label': category,
                'score': score
            })

        if predictions:
            asset.add_analysis('gcp-vision-content-moderation', {
                'predictions': predictions,
                'type': 'moderation',
                'safe': safe,
                'count': len(predictions)
            })


class CloudVisionDetectFaces(AbstractCloudVisionProcessor):
    """Executes face detection using the Cloud Vision API."""

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
        self.proxy_level = 3

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.face_detection(image=self.get_vision_image(proxy))
        faces = rsp.face_annotations

        if not faces:
            return

        # Make some rectangles
        rects = []
        for face in faces:
            rect = face.bounding_poly
            rects.append([rect.vertices[0].x,
                          rect.vertices[0].y,
                          rect.vertices[2].x,
                          rect.vertices[2].y])

        # Need this to normalize the rects
        pwidth = proxy.attrs['width']
        pheight = proxy.attrs['height']

        predictions = []
        for rect, face in zip(rects, faces):
            predictions.append({
                'bbox': calculate_normalized_bbox(pwidth, pheight, rect),
                'score': face.detection_confidence,
                'attributes': self.get_face_emotions(face)
            })

        struct = {
            'count': len(predictions),
            'type': 'faces',
            'predictions': predictions,
        }

        asset.add_analysis("gcp-vision-face-detection", struct)

    def get_face_emotions(self, face):
        labels = []
        for key in self.label_keys:
            if getattr(face, "{}_likelihood".format(key)) >= 4:
                labels.append(key)
        return labels


class CloudVisionDetectLabels(AbstractCloudVisionProcessor):
    """Executes Label Detection the Cloud Vision API."""

    def __init__(self):
        super(CloudVisionDetectLabels, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.label_detection(image=self.get_vision_image(proxy))
        labels = rsp.label_annotations
        if not labels:
            return

        predictions = []
        for label in labels:
            predictions.append({
                'label': label.description,
                'score': round(float(label.score), 3)
            })

        asset.add_analysis('gcp-vision-label-detection', {
            'predictions': predictions,
            'type': 'labels',
            'count': len(predictions)
        })


class CloudVisionDetectLogos(AbstractCloudVisionProcessor):
    """Executes Logo Detection the Cloud Vision API."""

    def __init__(self):
        super(CloudVisionDetectLogos, self).__init__()
        self.proxy_level = 2

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.logo_detection(image=self.get_vision_image(proxy))
        logos = rsp.logo_annotations
        if not logos:
            return

        pwidth = proxy.attrs["width"]
        pheight = proxy.attrs["height"]

        rects = []
        for logo in logos:
            rect = logo.bounding_poly
            rects.append([
                rect.vertices[0].x,
                rect.vertices[0].y,
                rect.vertices[2].x,
                rect.vertices[2].y])

        predictions = []
        for logo, rect in zip(logos, rects):
            predictions.append({
                'label': logo.description,
                'bbox': calculate_normalized_bbox(pwidth, pheight, rect),
                'score': round(float(logo.score), 3)
            })

        asset.add_analysis('gcp-vision-logo-detection', {
            'count': len(logos),
            'type': 'objects',
            'predictions': predictions
        })


class CloudVisionDetectObjects(AbstractCloudVisionProcessor):
    """Executes Object Detection the Cloud Vision API."""

    def __init__(self):
        super(CloudVisionDetectObjects, self).__init__()
        self.proxy_level = 2

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.object_localization(image=self.get_vision_image(proxy))
        objects = rsp.localized_object_annotations
        if not objects:
            return

        predictions = []
        for obj in objects:
            # build the bounding poly which is not a rect.
            poly = []
            for i in range(0, 4):
                poly.append(round(obj.bounding_poly.normalized_vertices[i].x, 4))
                poly.append(round(obj.bounding_poly.normalized_vertices[i].y, 4))

            predictions.append({
                'label': obj.name,
                'bbox': poly,
                'score': round(float(obj.score), 3)
            })

        asset.add_analysis('gcp-vision-object-detection', {
            'count': len(objects),
            'type': 'objects',
            'predictions': predictions
        })