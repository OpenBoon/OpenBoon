from azure.cognitiveservices.vision.computervision.models import VisualFeatureTypes

from zmlpsdk import Argument, AssetProcessor, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from .util import get_zvi_azure_cv_client

__all__ = [
    'ComputerVisionObjectDetection',
    'ComputerVisionLabelDetection',
    'ComputerVisionImageDescription',
    'ComputerVisionImageTagsDetection',
    'ComputerVisionCelebrityDetection',
    'ComputerVisionLandmarkDetection',
]


class AbstractComputerVisionProcessor(AssetProcessor):
    """
        This base class is used for all Microsoft Computer Vision features.  Subclasses
        only have to implement the "predict(asset, image) method.
        """

    file_types = FileTypes.images | FileTypes.documents

    def __init__(self, reactor=None):
        super(AbstractComputerVisionProcessor, self).__init__()
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


class ComputerVisionObjectDetection(AbstractComputerVisionProcessor):
    """Object detection for an image using Azure Computer Vision """

    namespace = 'azure-object-detection'

    def __init__(self):
        super(ComputerVisionObjectDetection, self).__init__()

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
        return [(r.object_property, r.confidence) for r in response.objects]


class ComputerVisionLabelDetection(AbstractComputerVisionProcessor):
    """Get labels for an image using Azure Computer Vision """

    namespace = 'azure-label-detection'

    def __init__(self):
        super(ComputerVisionLabelDetection, self).__init__()

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


class ComputerVisionImageDescription(AbstractComputerVisionProcessor):
    """Get image descriptions for an image using Azure Computer Vision """

    namespace = 'azure-image-description-detection'

    def __init__(self):
        super(ComputerVisionImageDescription, self).__init__()

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


class ComputerVisionImageTagsDetection(AbstractComputerVisionProcessor):
    """Get image tags for an image using Azure Computer Vision """

    namespace = 'azure-tag-detection'

    def __init__(self):
        super(ComputerVisionImageTagsDetection, self).__init__()

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


class ComputerVisionCelebrityDetection(AbstractComputerVisionProcessor):
    """Celebrity detection for an image using Azure Computer Vision """

    namespace = 'azure-celebrity-detection'
    model = "celebrities"

    def __init__(self):
        super(ComputerVisionCelebrityDetection, self).__init__()

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


class ComputerVisionLandmarkDetection(AbstractComputerVisionProcessor):
    """Landmark detection for an image using Azure Computer Vision """

    namespace = 'azure-landmark-detection'
    model = "landmarks"

    def __init__(self):
        super(ComputerVisionLandmarkDetection, self).__init__()

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
