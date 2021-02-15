import io

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import vision
from google.cloud.vision_v1 import types

from boonflow import file_storage, Argument, AssetProcessor, FileTypes
from boonflow.proxy import get_proxy_level, calculate_normalized_bbox
from boonflow.analysis import LabelDetectionAnalysis, ContentDetectionAnalysis, Prediction
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

    file_types = FileTypes.images | FileTypes.documents
    analysis_name = None

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

    def get_ocr_image(self, asset, fallback_proxy):
        """
        Loads the ocr-proxy into a Google Vision Image which is
        used for calling various vision services.  If there is no
        OCR proxy then the fallback proxy is used.

        Args:
            asset: (Asset): The asset with the proxy files.
            fallback_proxy (StoredFile): The StoredFile instance.

        Returns:
            google.cloud.vision.types.Image

        """
        ocr_proxy = asset.get_files(category='ocr-proxy')
        if ocr_proxy:
            return self.get_vision_image(ocr_proxy[0])
        else:
            return self.get_vision_image(fallback_proxy)


class CloudVisionDetectImageText(AbstractCloudVisionProcessor):
    """Executes Image Text Detection the Cloud Vision API."""

    analysis_name = 'gcp-vision-image-text-detection'

    def __init__(self):
        super(CloudVisionDetectImageText, self).__init__()
        self.image_level = 3

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        """Executes text detection on images using the Cloud Vision API."""
        # ImageText doesn't use an OCR proxy, t
        rsp = self.image_annotator.text_detection(image=self.get_ocr_image(asset, proxy))
        result = rsp.full_text_annotation

        text = result.text
        if not text:
            return

        analysis = ContentDetectionAnalysis()
        analysis.add_content(text)
        asset.add_analysis(self.analysis_name, analysis)


class CloudVisionDetectDocumentText(AbstractCloudVisionProcessor):
    """Executes Document Text Detection the Cloud Vision API."""

    analysis_name = 'gcp-vision-doc-text-detection'

    def __init__(self):
        super(CloudVisionDetectDocumentText, self).__init__()
        self.image_level = 3

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.document_text_detection(image=self.get_ocr_image(asset, proxy))
        text = rsp.full_text_annotation.text
        if not text:
            return

        analysis = ContentDetectionAnalysis()
        analysis.add_content(text)
        asset.add_analysis(self.analysis_name, analysis)


class CloudVisionDetectLandmarks(AbstractCloudVisionProcessor):
    """Executes landmark detection using the Cloud Vision API."""

    analysis_name = 'gcp-vision-landmark-detection'

    def __init__(self):
        super(CloudVisionDetectLandmarks, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.landmark_detection(image=self.get_vision_image(proxy))
        landmarks = rsp.landmark_annotations
        if not landmarks:
            return

        analysis = LabelDetectionAnalysis()

        for landmark in landmarks:
            analysis.add_prediction(Prediction(
                landmark.description,
                landmark.score,
                point={
                    'lat': landmark.locations[0].lat_lng.latitude,
                    'lon':  landmark.locations[0].lat_lng.longitude
                }
            ))

        asset.add_analysis(self.analysis_name, analysis)


class CloudVisionDetectExplicit(AbstractCloudVisionProcessor):
    """Executes Safe Search using the Cloud Vision API."""

    analysis_name = 'gcp-vision-content-moderation'

    def __init__(self):
        super(CloudVisionDetectExplicit, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.safe_search_detection(image=self.get_vision_image(proxy))
        result = rsp.safe_search_annotation

        analysis = LabelDetectionAnalysis()
        analysis.set_attr('explicit', False)

        for category in ['adult', 'spoof', 'medical', 'violence', 'racy']:
            rating = getattr(result, category)
            if rating <= 1 or rating > 5:
                continue
            score = (float(rating) - 1.0) * 0.25
            if score >= 0.50 and category in ('adult', 'racy'):
                analysis.set_attr('explicit', True)

            analysis.add_prediction(Prediction(category, score))

        if analysis:
            asset.add_analysis(self.analysis_name, analysis)


class CloudVisionDetectFaces(AbstractCloudVisionProcessor):
    """Executes face detection using the Cloud Vision API."""

    analysis_name = 'gcp-vision-face-detection'
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

        analysis = LabelDetectionAnalysis()
        face_num = 1
        for rect, face in zip(rects, faces):
            analysis.add_prediction(Prediction(
                "face%02d" % face_num,
                face.detection_confidence,
                bbox=calculate_normalized_bbox(pwidth, pheight, rect),
                tags=self.get_face_emotions(face)))

        asset.add_analysis(self.analysis_name, analysis)

    def get_face_emotions(self, face):
        labels = []
        for key in self.label_keys:
            if getattr(face, "{}_likelihood".format(key)) >= 4:
                labels.append(key)
        return labels


class CloudVisionDetectLabels(AbstractCloudVisionProcessor):
    """Executes Label Detection the Cloud Vision API."""

    analysis_name = 'gcp-vision-label-detection'

    def __init__(self):
        super(CloudVisionDetectLabels, self).__init__()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.label_detection(image=self.get_vision_image(proxy))
        labels = rsp.label_annotations
        if not labels:
            return

        analysis = LabelDetectionAnalysis()
        for label in labels:
            analysis.add_prediction(Prediction(
                label.description,
                label.score
            ))

        asset.add_analysis(self.analysis_name, analysis)


class CloudVisionDetectLogos(AbstractCloudVisionProcessor):
    """Executes Logo Detection the Cloud Vision API."""

    analysis_name = 'gcp-vision-logo-detection'

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

        analysis = LabelDetectionAnalysis()
        for logo, rect in zip(logos, rects):
            bbox = calculate_normalized_bbox(pwidth, pheight, rect)
            analysis.add_prediction(Prediction(logo.description, logo.score, bbox=bbox))

        asset.add_analysis(self.analysis_name, analysis)


class CloudVisionDetectObjects(AbstractCloudVisionProcessor):
    """Executes Object Detection the Cloud Vision API."""

    analysis_name = 'gcp-vision-object-detection'

    def __init__(self):
        super(CloudVisionDetectObjects, self).__init__()
        self.proxy_level = 1

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def detect(self, asset, proxy):
        rsp = self.image_annotator.object_localization(image=self.get_vision_image(proxy))
        objects = rsp.localized_object_annotations
        if not objects:
            return

        analysis = LabelDetectionAnalysis()
        for obj in objects:
            # build the bounding poly which is not a rect.
            poly = []
            for i in range(0, 4):
                poly.append(round(obj.bounding_poly.normalized_vertices[i].x, 4))
                poly.append(round(obj.bounding_poly.normalized_vertices[i].y, 4))

            analysis.add_prediction(
                Prediction(
                    obj.name,
                    obj.score,
                    bbox=poly
                )
            )

        asset.add_analysis(self.analysis_name, analysis)
