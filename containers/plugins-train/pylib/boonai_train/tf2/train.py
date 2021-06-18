import os
import shutil
import tempfile

import matplotlib.pyplot as plt
import tensorflow as tf
import tensorflow.keras.layers as layers
from tensorflow.keras.applications import efficientnet as efficientnet
from tensorflow.keras.applications import resnet_v2 as resnet_v2
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.preprocessing.image import ImageDataGenerator

from boonflow import ModelTrainer, Argument, file_storage
from boonflow.training import download_labeled_images


class TensorflowTransferLearningTrainer(ModelTrainer):
    file_types = None

    min_concepts = 2
    """The minimum number of concepts needed to train."""

    min_examples = 10
    """The minimum number of concepts needed to train."""

    def __init__(self):
        super(TensorflowTransferLearningTrainer, self).__init__()

        # These can be set optionally.
        self.add_arg(Argument("base_model", "str", required=True, default="efficientnet-b1",
                              toolTip="The base Keras model."))
        self.add_arg(Argument("epochs", "int", required=True, default=100,
                              toolTip="The number of training epochs"))
        self.add_arg(Argument("validation_split", "int", required=True, default=0.2,
                              toolTip="The number of training images vs test images"))
        self.add_arg(Argument("fine_tune_at_layer", "int", required=True, default=100,
                              toolTip="The layer to start find-tuning at."))
        self.add_arg(Argument("fine_tune_epochs", "int", required=True, default=100,
                              toolTip="The number of fine-tuning epochs."))

        self.model = None
        self.labels = None
        self.base_dir = None
        self.img_size = (224, 224)

    def init(self):
        self.img_size = self.get_image_size()
        self.load_app_model()
        self.labels = self.app.datasets.get_label_counts(self.app_model)
        self.base_dir = tempfile.mkdtemp('tf2-xfer-learning')
        self.check_labels()

    def get_image_size(self):
        base = self.arg_value('base_model')
        sizes = {
            'efficientnet-b0': (224, 224),
            'efficientnet-b1': (240, 240),
            'efficientnet-b2': (260, 260),
            'efficientnet-b3': (300, 300),
            'efficientnet-b4': (380, 380),
            'efficientnet-b5': (456, 456),
            'efficientnet-b6': (528, 528),
            'efficientnet-b7': (600, 600)
        }
        return sizes.get(base, (224, 224))

    def train(self):
        download_labeled_images(self.app_model,
                                "labels-standard",
                                self.base_dir)

        self.reactor.emit_status("Training model: {}".format(self.app_model.name))
        train_gen, test_gen = self.build_generators()
        self.train_model(train_gen, test_gen)

        # Build the label list
        labels = [None] * len(self.labels)
        for label, idx in train_gen.class_indices.items():
            labels[int(idx)] = label

        self.publish_model(labels)

    def plot_history(self, history, name):
        self.logger.info('Saving history plot.')
        acc = history.history['accuracy']
        val_acc = history.history['val_accuracy']

        loss = history.history['loss']
        val_loss = history.history['val_loss']

        plt.figure(figsize=(8, 8))
        plt.subplot(2, 1, 1)
        plt.plot(acc, label='Training Accuracy')
        plt.plot(val_acc, label='Validation Accuracy')
        plt.legend(loc='lower right')
        plt.ylabel('Accuracy')
        plt.ylim([min(plt.ylim()), 1])
        plt.title('Training and Validation Accuracy')

        plt.subplot(2, 1, 2)
        plt.plot(loss, label='Training Loss')
        plt.plot(val_loss, label='Validation Loss')
        plt.legend(loc='upper right')
        plt.ylabel('Cross Entropy')
        plt.ylim([0, 1.0])
        plt.title('Training and Validation Loss')
        plt.xlabel('epoch')

        with tempfile.NamedTemporaryFile(suffix=".png") as fp:
            plt.savefig(fp.name)
            file_storage.projects.store_file(fp.name,
                                             self.app_model, "model", rename=f'{name}.png')

    def publish_model(self, labels):
        """
        Publishes the trained model and a Pipeline Module which uses it.

        Args:
            labels (list): An array of labels in the correct order.

        """
        self.reactor.emit_status('Saving model: {}'.format(self.app_model.name))
        model_dir = tempfile.mkdtemp() + '/' + self.app_model.name
        os.makedirs(model_dir)

        self.model.save(model_dir)
        with open(model_dir + '/labels.txt', 'w') as fp:
            for label in labels:
                fp.write('{}\n'.format(label))

        tf2_dir = os.path.dirname(os.path.realpath(__file__))
        shutil.copy2(os.path.join(tf2_dir, "predict.py"), model_dir)
        mod = file_storage.models.save_model(model_dir, self.app_model, self.tag, self.post_action)
        self.reactor.emit_status('Published model: {}'.format(self.app_model.name))
        return mod

    def check_labels(self):
        """
        Check the labels to ensure we have enough labels and example images.

        """
        # Do some checks here.
        if len(self.labels) < self.min_concepts:
            raise ValueError('You need at least {} labels to train.'.format(self.min_concepts))

        for name, count in self.labels.items():
            if count < self.min_examples:
                msg = 'You need at least {} examples to train, {} has  {}'
                raise ValueError(msg.format(self.min_examples, name, count))

    def train_model(self, train_gen, test_gen):
        """
        Build and train Tensorflow model using the base model specified in the args.

        """

        # Make a new model from the base ResNet50 model.
        base_model = self.get_base_model()
        base_model.trainable = False

        self.model = self.build_model(base_model)
        self.model.summary()

        self.logger.info('Compiling...')
        self.model.compile(
            optimizer=Adam(),
            loss='categorical_crossentropy',
            metrics=['accuracy']
        )

        self.logger.info('Training...')
        epochs = int(self.arg_value('epochs'))

        history = self.model.fit(
            train_gen,
            callbacks=[HaltCallback(), TrainingProgressCallback(self.reactor, epochs)],
            validation_data=test_gen,
            epochs=epochs
        )

        self.plot_history(history, "history")

        # Number of epochs for fine tuning.
        fine_tune_at_layer = int(self.arg_value('fine_tune_at_layer'))
        fine_tune_epochs = int(self.arg_value('fine_tune_epochs'))

        if fine_tune_epochs <= 0:
            return

        # Now that we've trained our new layers, we're going to actually lightly retrain
        # all layers after the 100th layer in ResNet50.
        base_model.trainable = True

        # Freezes all the layers before the `fine_tune_at_layer` layer
        for layer in base_model.layers[:fine_tune_at_layer]:
            layer.trainable = False

        self.model.compile(loss='categorical_crossentropy',
                           optimizer=Adam(1e-4),
                           metrics=['accuracy'])

        history_fine = self.model.fit(train_gen,
                                      epochs=fine_tune_epochs,
                                      validation_data=test_gen,
                                      callbacks=[HaltCallback(), TrainingProgressCallback(
                                          self.reactor, fine_tune_epochs)])

        self.plot_history(history_fine, "history-fine-tune")

    def build_generators(self):
        """
        Build the tensorflow ImageDataGenerators used to load in the the train
        and test images.

        Returns:
            tuple: a tuple of ImageDataGenerators, 1st one is train, other is tet.
        """
        val_split = self.arg_value('validation_split')
        batch_size = 8
        data_dir = '{}/train/'.format(self.base_dir)

        train_gen = ImageDataGenerator(
            rescale=1. / 255,
            rotation_range=40,
            width_shift_range=0.2,
            height_shift_range=0.2,
            shear_range=0.2,
            zoom_range=0.2,
            horizontal_flip=True,
            fill_mode='nearest',
            validation_split=val_split
        )

        train_ds = train_gen.flow_from_directory(
            data_dir,
            batch_size=batch_size,
            class_mode='categorical',
            target_size=self.img_size,
            subset='training'
        )

        val_gen = ImageDataGenerator(
            rescale=1. / 255.,
            validation_split=val_split
        )

        val_ds = val_gen.flow_from_directory(
            data_dir,
            batch_size=batch_size,
            class_mode='categorical',
            target_size=self.img_size,
            subset='validation'
        )

        return train_ds, val_ds

    def get_base_model(self):
        """
        Return the base ResNet50 model.

        Returns:
            Model: A tensorflow model.

        Raises:
            FatalProcessorException: If the model is not fouond/

        """
        base = self.arg_value('base_model')
        shape = self.img_size + (3,)

        if base == 'resnet50_v2':
            return resnet_v2.ResNet50V2(
                weights='imagenet', include_top=False, input_shape=shape)
        elif base == 'resnet101_v2':
            return resnet_v2.ResNet101V2(
                weights='imagenet', include_top=False, input_shape=shape)
        elif base == 'resnet152_v2':
            return resnet_v2.ResNet152V2(
                weights='imagenet', include_top=False, input_shape=shape)
        elif base == 'efficientnet-b0':
            return efficientnet.EfficientNetB0(
                weights='imagenet', include_top=False, input_shape=shape)
        elif base == 'efficientnet-b1':
            return efficientnet.EfficientNetB1(
                weights='imagenet', include_top=False, input_shape=shape)
        elif base == 'efficientnet-b2':
            return efficientnet.EfficientNetB2(
                weights='imagenet', include_top=False, input_shape=shape)
        elif base == 'efficientnet-b3':
            return efficientnet.EfficientNetB3(
                weights='imagenet', include_top=False, input_shape=shape)
        elif base == 'efficientnet-b4':
            return efficientnet.EfficientNetB4(
                weights='imagenet', include_top=False, input_shape=shape)
        elif base == 'efficientnet-b5':
            return efficientnet.EfficientNetB5(
                weights='imagenet', include_top=False, input_shape=shape)
        else:
            raise RuntimeError(f'{base} is not a valid base model type')

    def build_model(self, base_model):
        base = self.arg_value('base_model')
        if 'resnet' in base:
            return tf.keras.models.Sequential([
                base_model,
                layers.Flatten(),
                layers.BatchNormalization(),
                layers.Dense(256, activation='relu'),
                layers.Dropout(0.5),
                layers.BatchNormalization(),
                layers.Dense(128, activation='relu'),
                layers.Dropout(0.5),
                layers.BatchNormalization(),
                layers.Dense(64, activation='relu'),
                layers.Dropout(0.5),
                layers.BatchNormalization(),
                layers.Dense(len(self.labels), activation='softmax')
            ])
        else:
            return tf.keras.models.Sequential([
                base_model,
                layers.GlobalAveragePooling2D(name="avg_pool"),
                layers.Dropout(0.2, name="top_dropout"),
                layers.Dense(len(self.labels), activation="softmax", name="pred")
            ])


class HaltCallback(tf.keras.callbacks.Callback):
    def on_epoch_end(self, epoch, logs={}):
        if logs.get('loss') <= 0.05:
            print("\n\n\nReached 0.05 loss value so cancelling training!\n\n\n")
            self.model.stop_training = True


class TrainingProgressCallback(tf.keras.callbacks.Callback):
    """
    A callback use to log training progress.
    """

    def __init__(self, reactor, epochs):
        super(TrainingProgressCallback, self).__init__()
        self.reactor = reactor
        self.epochs = epochs

    def on_epoch_begin(self, epoch, logs=None):
        self.reactor.emit_status('Training epoch {} of {}'.format(epoch, self.epochs))
