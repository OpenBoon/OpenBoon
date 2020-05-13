import tempfile
import warnings

import zmlp
from zmlpsdk import AssetProcessor, Argument
from ..utils.models import download_dataset
from zmlp_train.yolo3.convert import ConvertConfigs
from zmlp_train.yolo3.train import YOLOTrainer

warnings.filterwarnings("ignore")


class YOLOTransferLearningTrainer(AssetProcessor):
    """ YOLO v3 training """

    def __init__(self):
        super(YOLOTransferLearningTrainer, self).__init__()

        # These are the base args
        self.add_arg(Argument("model_id", "str", required=True,
                              toolTip="The model Id"))
        self.add_arg(Argument("config_path", "str", required=True,
                              default="yolo.cfg",
                              toolTip="Config file path for YOLOv3 model."))
        self.add_arg(Argument("weights_path", "str", required=True,
                              default="yolov3.weights",
                              toolTip="Weights file path for YOLOv3 model."))
        self.add_arg(Argument("output_path", "str", required=True,
                              default="model_data/yolo.h5",
                              toolTip="Output path to save for YOLOv3 model."))

        # These can be set optionally.
        self.add_arg(Argument("epochs", "int", required=True, default=10,
                              toolTip="The number of training epochs"))
        self.add_arg(Argument("min_concepts", "int", required=True, default=2,
                              toolTip="The min number of concepts needed to train."))
        self.add_arg(Argument("min_examples", "int", required=True, default=10,
                              toolTip="The min number of examples needed to train"))
        self.add_arg(
            Argument("train-test-ratio", "int", required=True, default=3,
                     toolTip="The number of training images vs test images"))

        self.app = zmlp.app_from_env()
        self.model = None
        self.labels = None
        self.base_dir = None

        self.config_path = self.arg_value('config_path')
        self.weights_path = self.arg_value('weights_path')
        self.output_path = self.arg_value('output_path')
        self.configs = ConvertConfigs(
            self.config_path,
            self.weights_path,
            self.output_path
        )

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.labels = self.app.datasets.get_label_counts(
            self.app_model.dataset_id)
        self.base_dir = tempfile.mkdtemp('yolo3-xfer-learning')
        self.check_labels()

    def check_labels(self):
        """
        Check the dataset labels to ensure we have enough labels and example images.

        """
        min_concepts = self.arg_value('min_concepts')
        min_examples = self.arg_value('min_examples')

        # Do some checks here.
        if len(self.labels) < min_concepts:
            raise ValueError(
                'You need at least {} labels to train.'.format(min_concepts))

        for name, count in self.labels.items():
            if count < min_examples:
                msg = 'You need at least {} examples to train, {} has  {}'
                raise ValueError(msg.format(min_examples, name, count))

    def process(self, frame):
        download_dataset(self.ds.id, self.base_dir,
                         self.arg_value('train-test-ratio'))
        # Reads Darknet config and weights
        # creates Keras model with TF backend
        self.configs.process()

        # YOLO Trainer
        yolo_trainer = YOLOTrainer(
            output_path=self.output_path,
            labels=list(self.labels.keys()),
            epochs=self.arg_value('epochs')
        )

        # Build the label list
        labels = [None] * len(self.labels)
        for label, idx in train_gen.class_indices.items():
            labels[int(idx)] = label
