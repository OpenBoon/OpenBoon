import os
import tempfile

import tensorflow as tf
from tensorflow.keras.applications import mobilenet_v2 as mobilenet_v2
from tensorflow.keras.applications import resnet_v2 as resnet_v2
from tensorflow.keras.applications import vgg16 as vgg16
from tensorflow.keras.layers import Dropout, Flatten, Dense, BatchNormalization
from tensorflow.keras.preprocessing.image import ImageDataGenerator

import zmlp
from zmlpsdk import AssetProcessor, Argument, ZmlpFatalProcessorException
from ..utils.models import upload_model_directory, download_dataset


class TensorflowTransferLearningTrainer(AssetProcessor):
    img_size = (224, 224)
    file_types = None

    def __init__(self):
        super(TensorflowTransferLearningTrainer, self).__init__()

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
        self.labels = None
        self.base_dir = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.labels = self.app.datasets.get_label_counts(self.app_model.dataset_id)
        self.base_dir = tempfile.mkdtemp('tf2-xfer-learning')
        self.check_labels()

    def process(self, frame):
        self.reactor.write_event("status", {
            "status": "Downloading files in DataSet"
        })
        download_dataset("labels_std",
                         self.app_model.dataset_id,
                         self.base_dir,
                         self.arg_value('train-test-ratio'))

        self.reactor.write_event("status", {
            "status": "Training model{}".format(self.app_model.file_id)
        })
        self.build_model()
        train_gen, test_gen = self.build_generators()
        self.model.fit_generator(
            train_gen,
            validation_data=test_gen,
            epochs=self.arg_value('epochs')
        )

        # Build the label list
        labels = [None] * len(self.labels)
        for label, idx in train_gen.class_indices.items():
            labels[int(idx)] = label

        self.publish_model(labels)

    def publish_model(self, labels):
        """
        Publishes the trained model and a Pipeline Module which uses it.

        Args:
            labels (list): An array of labels in the correct order.

        """
        self.logger.info('publishing model')
        model_dir = tempfile.mkdtemp() + '/' + self.app_model.name
        os.makedirs(model_dir)

        self.logger.info('saving model : {}'.format(model_dir))
        self.model.save(model_dir)
        with open(model_dir + '/labels.txt', 'w') as fp:
            for label in labels:
                fp.write('{}\n'.format(label))

        # Upload the zipped model to project storage.
        self.logger.info('uploading model')

        self.reactor.write_event("status", {
            "status": "Uploading model{}".format(self.app_model.file_id)
        })

        upload_model_directory(model_dir, self.app_model.file_id)

        self.app.models.publish_model(self.app_model)
        self.reactor.write_event("status", {
            "status": "Published model {}".format(self.app_model.file_id)
        })

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

    def build_model(self):
        """
        Build the Tensorflow model using the base model specified in the args.
        """
        self.reactor.write_event("status", {
            "status": "Building model{}".format(self.app_model.file_id)
        })

        base_model = self.get_base_model()

        base_model.trainable = False
        for layer in base_model.layers:
            layer.trainable = False

        self.model = tf.keras.models.Sequential([
            base_model,

            Flatten(),
            Dense(512, activation='relu'),
            BatchNormalization(),
            Dropout(0.5),

            Dense(64, activation='relu'),
            BatchNormalization(),
            Dropout(0.5),
            Dense(len(self.labels), activation='softmax')
        ])

        self.model.summary()
        self.logger.info('Compiling...')

        self.model.compile(
            optimizer='adam',
            loss='categorical_crossentropy',
            metrics=['acc']
        )

    def build_generators(self):
        """
        Build the tensorflow ImageDataGenerators used to load in the the train
        and test images.

        Returns:
            tuple: a tuple of ImageDataGenerators, 1st one is train, other is tet.
        """
        train_datagen = ImageDataGenerator(
            rescale=1. / 255,
            rotation_range=40,
            width_shift_range=0.2,
            height_shift_range=0.2,
            shear_range=0.2,
            zoom_range=0.2,
            horizontal_flip=True,
            fill_mode='nearest'
        )

        test_datagen = ImageDataGenerator(rescale=1. / 255.)

        train_generator = train_datagen.flow_from_directory(
            '{}/set_train/'.format(self.base_dir),
            batch_size=128,
            class_mode='categorical',
            target_size=self.img_size
        )

        test_generator = test_datagen.flow_from_directory(
            '{}/set_test/'.format(self.base_dir),
            batch_size=128,
            class_mode='categorical',
            target_size=self.img_size
        )

        return train_generator, test_generator

    def get_base_model(self):
        """
        Using the 'base_model' arg, choose the base model for transfer learning/

        Returns:
            Model: A tensorflow model.

        Raises:
            ZmlpFatalProcessorException: If the model is not fouond/

        """
        modl_base = self.app_model.type
        if modl_base == zmlp.ModelType.LABEL_DETECTION_MOBILENET2:
            return mobilenet_v2.MobileNetV2(weights='imagenet',
                                            include_top=False,
                                            input_shape=(224, 224, 3))
        elif modl_base == zmlp.ModelType.LABEL_DETECTION_RESNET152:
            return resnet_v2.ResNet152V2(weights='imagenet',
                                         include_top=False,
                                         input_shape=(224, 224, 3))
        elif modl_base == zmlp.ModelType.LABEL_DETECTION_VGG16:
            return vgg16.VGG16(weights='imagenet',
                               include_top=False,
                               input_shape=(224, 224, 3))
        else:
            raise ZmlpFatalProcessorException('Invalid model: {}'.format(modl_base))
