import os
import tempfile

import matplotlib.pyplot as plt
import tensorflow as tf
from tensorflow.keras.applications import resnet_v2 as resnet_v2
from tensorflow.keras.layers import Dense, GlobalAveragePooling2D
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.preprocessing.image import ImageDataGenerator

import zmlp
from zmlpsdk import AssetProcessor, Argument, file_storage
from zmlpsdk.training import download_labeled_images


class TensorflowTransferLearningTrainer(AssetProcessor):
    img_size = (224, 224)
    file_types = None

    min_concepts = 2
    """The minimum number of concepts needed to train."""

    min_examples = 10
    """The minimum number of concepts needed to train."""

    def __init__(self):
        super(TensorflowTransferLearningTrainer, self).__init__()

        # These are the base args
        self.add_arg(Argument("model_id", "str", required=True,
                              toolTip="The model Id"))
        self.add_arg(Argument("deploy", "bool", default=False,
                              toolTip="Automatically deploy the model onto assets."))

        # These can be set optionally.
        self.add_arg(Argument("epochs", "int", required=True, default=12,
                              toolTip="The number of training epochs"))
        self.add_arg(Argument("training_set_split", "int", required=True, default=3,
                              toolTip="The number of training images vs test images"))
        self.add_arg(Argument("fine_tune_at_layer", "int", required=True, default=100,
                              toolTip="The layer to start find-tuning at."))
        self.add_arg(Argument("fine_tune_epochs", "int", required=True, default=7,
                              toolTip="The number of fine-tuning epochs."))

        self.app = zmlp.app_from_env()

        self.model = None
        self.labels = None
        self.base_dir = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.labels = self.app.models.get_label_counts(self.app_model)
        self.base_dir = tempfile.mkdtemp('tf2-xfer-learning')
        self.check_labels()

    def process(self, frame):
        download_labeled_images(self.app_model,
                                "labels_std",
                                self.base_dir,
                                self.arg_value('training_set_split'))

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
        fname = f'/tmp/{name}.png'
        plt.savefig(fname)

        file_storage.projects.store_file(fname, self.app_model, "model")

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

        mod = file_storage.models.save_model(model_dir,  self.app_model, self.arg_value('deploy'))
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

        self.model = tf.keras.models.Sequential([
            base_model,
            tf.keras.layers.Conv2D(32, 3, activation='relu'),
            tf.keras.layers.Dropout(0.2),
            GlobalAveragePooling2D(),
            Dense(len(self.labels), activation='softmax')
        ])

        self.model.summary()

        self.logger.info('Compiling...')
        self.model.compile(
            optimizer=Adam(),
            loss='categorical_crossentropy',
            metrics=['accuracy']
        )

        self.logger.info('Training...')
        history = self.model.fit(
            train_gen,
            callbacks=[TrainingProgressCallback(self.reactor, self.arg_value('epochs'))],
            validation_data=test_gen,
            epochs=self.arg_value('epochs')
        )

        self.plot_history(history, "history")

        # Number of epochs for fine tuning.
        fine_tune_at_layer = self.arg_value('fine_tune_at_layer')
        fine_tune_epochs = self.arg_value('fine_tune_epochs')

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
                                      callbacks=[TrainingProgressCallback(
                                          self.reactor, fine_tune_epochs)])

        self.plot_history(history_fine, "history-fine-tune")

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

        # increasing batch size increases memory usage.
        train_generator = train_datagen.flow_from_directory(
            '{}/set_train/'.format(self.base_dir),
            batch_size=8,
            class_mode='categorical',
            target_size=self.img_size
        )

        test_generator = test_datagen.flow_from_directory(
            '{}/set_validate/'.format(self.base_dir),
            batch_size=8,
            class_mode='categorical',
            target_size=self.img_size
        )

        return train_generator, test_generator

    @staticmethod
    def get_base_model():
        """
        Return the base ResNet50 model.

        Returns:
            Model: A tensorflow model.

        Raises:
            ZmlpFatalProcessorException: If the model is not fouond/

        """
        return resnet_v2.ResNet50V2(weights='imagenet',
                                    include_top=False,
                                    input_shape=(224, 224, 3))


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
