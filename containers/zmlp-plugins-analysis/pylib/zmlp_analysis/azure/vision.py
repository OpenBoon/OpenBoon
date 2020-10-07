from azure.cognitiveservices.vision.computervision.models import VisualFeatureTypes

from zmlpsdk import Argument, AssetProcessor, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from .util import get_zvi_azure_cv_client

__all__ = [
    'AzureVisionObjectDetection',
    'AzureVisionLabelDetection',
    'AzureVisionImageDescription',
    'AzureVisionImageTagsDetection',
    'AzureVisionCelebrityDetection',
    'AzureVisionLandmarkDetection',
    'AzureVisionLogoDetection',
    'AzureVisionCategoryDetection',
    'AzureVisionExplicitContentDetection',

]


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
            asset.add_analysis(self.namespace, analysis)
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

        try:
            asset.add_analysis(self.namespace, analysis)
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
            labels.append((r.object_property, r.confidence, bbox))
        return labels


class AzureVisionLabelDetection(AbstractAzureVisionProcessor):
    """Get labels for an image using Azure Computer Vision """

    namespace = 'azure-label-detection'

    def __init__(self):
        super(AzureVisionLabelDetection, self).__init__()

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


class AzureVisionImageDescription(AbstractAzureVisionProcessor):
    """Get image descriptions for an image using Azure Computer Vision """

    namespace = 'azure-image-description-detection'

    def __init__(self):
        super(AzureVisionImageDescription, self).__init__()

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

        try:
            asset.add_analysis(self.namespace, analysis)
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
            labels.append((r['name'], r['confidence'], bbox))
        return labels


class AzureVisionLandmarkDetection(AbstractAzureVisionProcessor):
    """Landmark detection for an image using Azure Computer Vision """

    namespace = 'azure-landmark-detection'
    model = "landmarks"

    def __init__(self):
        super(AzureVisionLandmarkDetection, self).__init__()

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

        try:
            asset.add_analysis(self.namespace, analysis)
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
            labels.append((r.name, r.confidence, bbox))
        return labels


class AzureVisionCategoryDetection(AbstractAzureVisionProcessor):
    """Category detection for an image using Azure Computer Vision """

    image_features = ['categories']
    namespace = 'azure-category-detection'

    def __init__(self):
        super(AzureVisionCategoryDetection, self).__init__()

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
