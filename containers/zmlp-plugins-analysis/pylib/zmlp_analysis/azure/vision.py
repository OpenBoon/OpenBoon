import time
from PIL import Image

import backoff

from azure.cognitiveservices.vision.computervision.models import \
    VisualFeatureTypes, OperationStatusCodes, ComputerVisionErrorException

from zmlpsdk import Argument, AssetProcessor, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis, ContentDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path, get_proxy_level, calculate_normalized_bbox
from zmlpsdk import file_storage

from .util import get_zvi_azure_cv_client

__all__ = [
    'AzureVisionObjectDetection',
    'AzureVisionLabelDetection',
    'AzureVisionImageDescriptionDetection',
    'AzureVisionImageTagsDetection',
    'AzureVisionCelebrityDetection',
    'AzureVisionLandmarkDetection',
    'AzureVisionLogoDetection',
    'AzureVisionCategoryDetection',
    'AzureVisionExplicitContentDetection',
    'AzureVisionFaceDetection',
    'AzureVisionTextDetection'
]


def not_a_quota_exception(exp):
    """
    Returns true if the exception is not an Azure quota exception.  This ensures the backoff
    function doesn't sleep on the wrong exceptions.

    Args:
        exp (Exception): The exception

    Returns:
        bool: True if not a quota exception.
    """
    return 'Too Many Requests' not in str(exp)


class AbstractAzureVisionProcessor(AssetProcessor):
    """
    This base class is used for all Microsoft Computer Vision features.  Subclasses
    only have to implement the "predict(asset, image) method.
    """

    file_types = FileTypes.images | FileTypes.documents

    def __init__(self, reactor=None):
        super(AbstractAzureVisionProcessor, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.reactor = reactor

        # The Azure client
        self.client = None

    def init(self):
        # Azure Computer Vision Client
        self.client = get_zvi_azure_cv_client()

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        """
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        analysis = LabelDetectionAnalysis()

        predictions = self.predict(proxy_path)
        for ls in predictions:
            analysis.add_label_and_score(ls[0], ls[1])

        try:
            self.add_analysis(asset, self.namespace, analysis)
        except NameError:
            self.reactor.emit_status("self.namespace not defined")

    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        raise NotImplementedError

    def emit_status(self, msg):
        """
        Emit a status back to the Archivist.

        Args:
            msg (str): The message to emit.

        """
        if not self.reactor:
            return
        self.reactor.emit_status(msg)


class AzureVisionObjectDetection(AbstractAzureVisionProcessor):
    """Object detection for an image using Azure Computer Vision """

    namespace = 'azure-object-detection'

    def __init__(self):
        super(AzureVisionObjectDetection, self).__init__()

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        """
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        analysis = LabelDetectionAnalysis()

        predictions = self.predict(proxy_path)
        for ls in predictions:
            analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2])

        self.add_analysis(asset, self.namespace, analysis)

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        # get height and width of image
        image = Image.open(path)
        img_width, img_height = image.size

        with open(path, 'rb') as img:
            response = self.client.detect_objects_in_stream(image=img)

        # get list of labels
        labels = []
        for r in response.objects:
            bbox = [
                r.rectangle.x,
                r.rectangle.y,
                r.rectangle.x + r.rectangle.w,
                r.rectangle.y + r.rectangle.h,
            ]
            normalized_bbox = calculate_normalized_bbox(img_width, img_height, bbox)
            labels.append((r.object_property, r.confidence, normalized_bbox))
        return labels


class AzureVisionLabelDetection(AbstractAzureVisionProcessor):
    """Get labels for an image using Azure Computer Vision """

    namespace = 'azure-label-detection'

    def __init__(self):
        super(AzureVisionLabelDetection, self).__init__()

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        with open(path, 'rb') as img:
            response = self.client.analyze_image_in_stream(
                image=img,
                visual_features=[VisualFeatureTypes.tags]
            )

        # get list of labels
        return [(r.name, r.confidence) for r in response.tags]


class AzureVisionImageDescriptionDetection(AbstractAzureVisionProcessor):
    """Get image descriptions for an image using Azure Computer Vision """

    namespace = 'azure-image-description-detection'

    def __init__(self):
        super(AzureVisionImageDescriptionDetection, self).__init__()

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        with open(path, 'rb') as img:
            response = self.client.describe_image_in_stream(image=img)

        # get list of labels
        return [(r.text, r.confidence) for r in response.captions]


class AzureVisionImageTagsDetection(AbstractAzureVisionProcessor):
    """Get image tags for an image using Azure Computer Vision """

    namespace = 'azure-tag-detection'

    def __init__(self):
        super(AzureVisionImageTagsDetection, self).__init__()

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        with open(path, 'rb') as img:
            response = self.client.tag_image_in_stream(image=img)

        # get list of labels
        return [(r.name, r.confidence) for r in response.tags]


class AzureVisionCelebrityDetection(AbstractAzureVisionProcessor):
    """Celebrity detection for an image using Azure Computer Vision """

    namespace = 'azure-celebrity-detection'
    model = "celebrities"

    def __init__(self):
        super(AzureVisionCelebrityDetection, self).__init__()

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        """
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        analysis = LabelDetectionAnalysis()

        predictions = self.predict(proxy_path)
        for ls in predictions:
            analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2])

        self.add_analysis(asset, self.namespace, analysis)

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        # get height and width of image
        image = Image.open(path)
        img_width, img_height = image.size

        with open(path, 'rb') as img:
            response = self.client.analyze_image_by_domain_in_stream(model=self.model, image=img)

        # get list of labels
        labels = []
        for r in response.result[self.model]:
            bbox = [
                r['faceRectangle']['left'],
                r['faceRectangle']['top'],
                r['faceRectangle']['left'] + r['faceRectangle']['width'],
                r['faceRectangle']['top'] + r['faceRectangle']['height'],
            ]
            normalized_bbox = calculate_normalized_bbox(img_width, img_height, bbox)
            labels.append((r['name'], r['confidence'], normalized_bbox))
        return labels


class AzureVisionLandmarkDetection(AbstractAzureVisionProcessor):
    """Landmark detection for an image using Azure Computer Vision """

    namespace = 'azure-landmark-detection'
    model = "landmarks"

    def __init__(self):
        super(AzureVisionLandmarkDetection, self).__init__()

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        with open(path, 'rb') as img:
            response = self.client.analyze_image_by_domain_in_stream(model=self.model, image=img)

        # get list of labels
        return [(r['name'], r['confidence']) for r in response.result[self.model]]


class AzureVisionLogoDetection(AbstractAzureVisionProcessor):
    """Logo detection for an image using Azure Computer Vision """

    image_features = ['brands']
    namespace = 'azure-logo-detection'

    def __init__(self):
        super(AzureVisionLogoDetection, self).__init__()

    def process(self, frame):
        """
        Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        """
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        analysis = LabelDetectionAnalysis()

        predictions = self.predict(proxy_path)
        for ls in predictions:
            analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2])

        self.add_analysis(asset, self.namespace, analysis)

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        # get height and width of image
        image = Image.open(path)
        img_width, img_height = image.size

        with open(path, 'rb') as img:
            response = self.client.analyze_image_in_stream(
                image=img,
                visual_features=self.image_features
            )

        # get list of labels
        labels = []
        for r in response.brands:
            bbox = [
                r.rectangle.x,
                r.rectangle.y,
                r.rectangle.x + r.rectangle.w,
                r.rectangle.y + r.rectangle.h,
            ]
            normalized_bbox = calculate_normalized_bbox(img_width, img_height, bbox)
            labels.append((r.name, r.confidence, normalized_bbox))
        return labels


class AzureVisionCategoryDetection(AbstractAzureVisionProcessor):
    """Category detection for an image using Azure Computer Vision """

    image_features = ['categories']
    namespace = 'azure-category-detection'

    def __init__(self):
        super(AzureVisionCategoryDetection, self).__init__()

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        with open(path, 'rb') as img:
            response = self.client.analyze_image_in_stream(
                image=img,
                visual_features=self.image_features
            )

        # get list of labels
        return [(r.name, r.score) for r in response.categories]


class AzureVisionExplicitContentDetection(AbstractAzureVisionProcessor):
    """Explicit Content detection for an image using Azure Computer Vision """

    image_features = ['adult']
    namespace = 'azure-explicit-detection'

    def __init__(self):
        super(AzureVisionExplicitContentDetection, self).__init__()

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        with open(path, 'rb') as img:
            response = self.client.analyze_image_in_stream(
                image=img,
                visual_features=self.image_features
            )

        # get list of labels
        return [
            ('adult', response.adult.adult_score),
            ('racy', response.adult.racy_score),
            ('gory', response.adult.gore_score),
        ]


class AzureVisionFaceDetection(AbstractAzureVisionProcessor):
    """Logo detection for an image using Azure Computer Vision """

    image_features = ['faces']
    namespace = 'azure-face-detection'

    def __init__(self):
        super(AzureVisionFaceDetection, self).__init__()

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        """
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        analysis = LabelDetectionAnalysis()

        predictions = self.predict(proxy_path)
        for ls in predictions:
            analysis.add_label_and_score(ls[0], ls[1], bbox=ls[2], age=ls[3])

        self.add_analysis(asset, self.namespace, analysis)

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        # get height and width of image
        image = Image.open(path)
        img_width, img_height = image.size

        with open(path, 'rb') as img:
            response = self.client.analyze_image_in_stream(
                image=img,
                visual_features=self.image_features
            )

        # get list of labels
        labels = []
        for r in response.faces:
            bbox = [
                r.face_rectangle.left,
                r.face_rectangle.top,
                r.face_rectangle.left + r.face_rectangle.width,
                r.face_rectangle.top + r.face_rectangle.height,
            ]
            normalized_bbox = calculate_normalized_bbox(img_width, img_height, bbox)
            labels.append((r.gender, '1.00', normalized_bbox, r.age))
        return labels


class AzureVisionTextDetection(AbstractAzureVisionProcessor):
    """Get OCR'd text for an image using Azure Computer Vision """

    namespace = 'azure-image-text-detection'

    def __init__(self):
        super(AzureVisionTextDetection, self).__init__()

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        """
        asset = frame.asset
        proxy_path = self.get_ocr_image(asset)
        analysis = ContentDetectionAnalysis()

        text = self.predict(proxy_path)
        analysis.add_content(text)

        self.add_analysis(asset, self.namespace, analysis)

    @backoff.on_exception(backoff.expo,
                          ComputerVisionErrorException,
                          max_time=3600,
                          giveup=not_a_quota_exception)
    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            list: a list of predictions
        """
        with open(path, 'rb') as img:
            response = self.client.read_in_stream(image=img, raw=True)

        # Get the operation location (URL with an ID at the end) from the response
        operation_location_remote = response.headers["Operation-Location"]

        # Grab the ID from the URL
        operation_id = operation_location_remote.split("/")[-1]

        # Call the "GET" API and wait for it to retrieve the results
        while True:
            results = self.client.get_read_result(operation_id)
            if results.status not in ['notStarted', 'running']:
                break
            time.sleep(1)

        # Print the detected text, line by line
        lines = []
        if results.status == OperationStatusCodes.succeeded:
            for text_result in results.analyze_result.read_results:
                for line in text_result.lines:
                    lines.append(line.text.strip())

        # get text in a single string
        return ' '.join(lines)

    def get_ocr_image(self, asset):
        """
        Fetch the proper OCR image

        Args:
            asset (Asset): The Asset

        Returns:
            str: The location of the file.
        """
        ocr_proxy = asset.get_files(category='ocr-proxy')
        if ocr_proxy:
            return file_storage.localize_file(ocr_proxy)
        else:
            return file_storage.localize_file(get_proxy_level(asset, 0))
