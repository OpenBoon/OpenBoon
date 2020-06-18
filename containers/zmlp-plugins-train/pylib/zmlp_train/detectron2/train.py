"""Detectron2 Trainer"""

import tempfile
import glob
import os
import shutil
import ntpath
import numpy as np
import cv2
import random
import itertools
import pandas as pd
import urllib
import json
import PIL.Image as Image

import torch, torchvision
import detectron2
from detectron2.utils.logger import setup_logger
from detectron2 import model_zoo
from detectron2.engine import DefaultPredictor, DefaultTrainer
from detectron2.config import get_cfg
from detectron2.utils.visualizer import Visualizer, ColorMode
from detectron2.data import (
    DatasetCatalog,
    MetadataCatalog,
    build_detection_test_loader,
)
from detectron2.data.datasets import register_coco_instances
from detectron2.evaluation import COCOEvaluator, inference_on_dataset
from detectron2.structures import BoxMode

import zmlp
from zmlpsdk import AssetProcessor, Argument, file_storage
from zmlpsdk.training import download_dataset
from zmlp.training import DataSetDownloader

setup_logger()


class Detectron2TransferLearningTrainer(AssetProcessor):

    def __init__(self):
        super(Detectron2TransferLearningTrainer, self).__init__()

        # These are the base args
        self.add_arg(Argument("model_id", "str", required=True,
                              toolTip="The model Id"))

        # These can be set optionally.
        self.add_arg(Argument("epochs", "int", required=True, default=10,
                              toolTip="The number of training epochs"))
        self.add_arg(Argument("min_concepts", "int", required=True, default=2,
                              toolTip="The min number of concepts needed to train."))
        self.add_arg(Argument("min_examples", "int", required=True, default=10,
                              toolTip="The min number of examples needed to train"))
        self.add_arg(Argument("train-test-ratio", "int", required=True, default=3,
                              toolTip="The number of training images vs test images"))
        self.app = zmlp.app_from_env()

        self.model = None
        self.dataset = None
        self.labels = None
        self.base_dir = None
        self.cfg = None
        self.trainer = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.epochs = self.arg_value('epochs')
        self.dataset = self.app_model.dataset_id
        self.labels = self.app.datasets.get_label_counts(self.dataset)
        self.base_dir = tempfile.mkdtemp('tf2-xfer-learning')
        self.check_labels()

    def process(self, frame):
        download_dataset(self.app_model.dataset_id,
                         "labels_std",
                         self.base_dir,
                         self.arg_value('train-test-ratio'))

        self.reactor.emit_status("Training model: {}".format(self.app_model.name))

        self.register_dataset(set_test=False)
        self.train()

        self.publish_model(self.labels)

    def check_labels(self):
        """
        Check the dataset labels to ensure we have enough labels and example images.

        """
        min_concepts = self.arg_value('min_concepts')
        min_examples = self.arg_value('min_examples')

        # Do some checks here.
        if len(self.labels) < min_concepts:
            raise ValueError('You need at least {} labels to train.'.format(min_concepts))

        for name, count in self.labels.items():
            if count < min_examples:
                msg = 'You need at least {} examples to train, {} has  {}'
                raise ValueError(msg.format(min_examples, name, count))

    def register_dataset(self, set_test=False):
        """Register COCO Dataset for Detectron2

        Args:
            set_test: register test dir

        Returns:
            None
        """
        dsl = DataSetDownloader(self.app, self.dataset, 'objects_coco', self.base_dir)
        dsl.build()

        json_file = os.path.join(self.base_dir, dsl.SET_TRAIN, 'annotations.json')
        image_root = os.path.join(self.base_dir, dsl.SET_TRAIN, 'images')
        register_coco_instances("{}_train".format(self.dataset), {}, json_file, image_root)

        if set_test:
            json_file = os.path.join(self.base_dir, dsl.SET_TEST, 'annotations.json')
            image_root = os.path.join(self.base_dir, dsl.SET_TEST, 'images')
            register_coco_instances("{}_test".format(self.dataset), {}, json_file, image_root)

    def train(self):
        """Start training

        Returns:
            None
        """
        self.cfg = self.config(
            train="{}_train".format(self.dataset),
            test="{}_test".format(self.dataset),
            set_test=False,
            num_classes=len(self.labels),
            epochs=self.epochs
        )
        self.cfg.OUTPUT_DIR = os.path.join(self.base_dir, 'output')
        os.makedirs(self.cfg.OUTPUT_DIR, exist_ok=True)

        self.trainer = DefaultTrainer(self.cfg)
        self.trainer.resume_or_load(resume=False)
        self.trainer.train()

    def config(
            self,
            config_file="",
            train="",
            test="",
            set_test=False,
            num_classes=0,
            epochs=100,
            lr=0.001,
            gamma=0.05,
    ):
        """Set up Detectron2 configs

        Args:
            config_file: (str) base model
            train: (str) train dir
            test: (str) val dir
            set_test: (bool) add test dir (default: False)
            num_classes: (int) number of labels
            epochs: (int) epochs to run (default: 100)
            lr: (float) learning rate (default: 0.001)
            gamma: (float) gamma (default: 0.05)

        Returns:
            (CfgNode) Detectron2 CfgNode instance with specified configurations
        """
        if not config_file:
            # config_file = "COCO-InstanceSegmentation/mask_rcnn_R_50_FPN_3x.yaml"
            config_file = "COCO-Detection/retinanet_R_50_FPN_1x.yaml"

        cfg = get_cfg()

        # add project-specific config (e.g., TensorMask) here if not running
        # a model in detectron2's core library
        cfg.merge_from_file(model_zoo.get_config_file(config_file))

        # Model from detectron2's model zoo
        cfg.MODEL.WEIGHTS = model_zoo.get_checkpoint_url(config_file)

        if not torch.cuda.is_available():
            cfg.MODEL.DEVICE = "cpu"

        # Specify datasets to use for training and evaluation
        cfg.DATASETS.TRAIN = (train,)
        cfg.DATASETS.TEST = (test,) if set_test else ()
        cfg.DATALOADER.NUM_WORKERS = 4

        # Optimizer
        warmup_iters = epochs * 0.667
        cfg.SOLVER.IMS_PER_BATCH = 4
        cfg.SOLVER.BASE_LR = lr
        cfg.SOLVER.GAMMA = gamma
        # the learning rate starts from 0 and goes to `lr` for `warmup_iters`
        # iterations
        cfg.SOLVER.WARMUP_ITERS = warmup_iters
        cfg.SOLVER.MAX_ITER = epochs
        # The iteration number to decrease learning rate by GAMMA
        cfg.SOLVER.STEPS = (warmup_iters, epochs)

        # Evaluation
        cfg.MODEL.ROI_HEADS.BATCH_SIZE_PER_IMAGE = 64
        cfg.MODEL.ROI_HEADS.NUM_CLASSES = num_classes

        # The period (in terms of steps) to evaluate the model during training.
        # Set to 0 to disable.
        cfg.TEST.EVAL_PERIOD = epochs * 0.333

        return cfg

    def publish_model(self, labels):
        """
        Publishes the trained model and a Pipeline Module which uses it.

        Args:
            labels (list): An array of labels in the correct order.

        """
        self.reactor.emit_status("Saving model: {}".format(self.app_model.name))
        model_dir = tempfile.mkdtemp() + '/' + self.app_model.name
        os.makedirs(model_dir, exist_ok=True)

        model_pth = os.path.join(self.cfg.OUTPUT_DIR, "model_final.pth")
        shutil.copy(src=model_pth, dst=model_dir)
        with open(model_dir + '/labels.txt', 'w') as fp:
            for label in labels:
                fp.write('{}\n'.format(label))

        mod = file_storage.models.save_model(model_dir,  self.app_model)
        self.reactor.emit_status("Published model: {}".format(self.app_model.name))
        return mod

    def evaluate(self, cfg=None, trainer=None, test="", threshold=0.75):
        """Evaluate a trained model

        Args:
            cfg: (CfgNode) model configurations
            trainer: (DefaultTrainer) trained model
            test: (str) val dir (should be same as cfg's test dir)
            threshold: (float) minimum threshold of certainty (default: 0.75)

        Returns:
            (DefaultPredictor) predictor with the given config that runs on single device for a
            single input image
        """
        cfg.MODEL.WEIGHTS = os.path.join(cfg.OUTPUT_DIR, "model_final.pth")
        cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = threshold
        predictor = DefaultPredictor(cfg)

        evaluator = COCOEvaluator(test, cfg, False, output_dir="./output/")
        val_loader = build_detection_test_loader(cfg, test)
        inference_on_dataset(trainer.model, val_loader, evaluator)

        return predictor

    def predict(self, cfg=None, predictor=None, img=None):
        """Predict on an image and return result

        Args:
            cfg: (CfgNode) model configurations
            predictor: (DefaultPredictor) predictor with the given config (output of self.evaluate)
            img: (str) image path

        Returns:
            (ndarray) the visualized image of shape (H, W, 3) (RGB) in uint8 type
        """
        im = cv2.imread(img)
        outputs = predictor(im)
        v = Visualizer(
            im[:, :, ::-1],
            MetadataCatalog.get(cfg.DATASETS.TRAIN[0]),
            scale=1.,
            instance_mode=ColorMode.IMAGE
        )

        instances = outputs["instances"].to("cpu")
        instances.remove('pred_masks')
        v = v.draw_instance_predictions(instances)
        result = v.get_image()[:, :, ::-1]

        return result
