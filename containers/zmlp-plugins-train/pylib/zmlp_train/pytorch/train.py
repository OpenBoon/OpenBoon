import os
import tempfile

import matplotlib.pyplot as plt

import torch
import torch.nn as nn
import torch.optim as optim
from torch.optim import lr_scheduler
from torchvision import datasets, models, transforms
import copy
import time


import zmlp
from zmlpsdk import AssetProcessor, Argument, file_storage
from zmlpsdk.training import download_labeled_images


class PytorchTransferLearningTrainer(AssetProcessor):
    img_size = (224, 224)
    file_types = None

    min_concepts = 2
    """The minimum number of concepts needed to train."""

    min_examples = 10
    """The minimum number of concepts needed to train."""

    def __init__(self):
        super(PytorchTransferLearningTrainer, self).__init__()

        # These are the base args
        self.add_arg(Argument("model_id", "str", required=True,
                              toolTip="The model Id"))
        self.add_arg(Argument("deploy", "bool", default=False,
                              toolTip="Automatically deploy the model onto assets."))

        # These can be set optionally.
        self.add_arg(Argument("epochs", "int", required=True, default=10,
                              toolTip="The number of training epochs"))
        self.add_arg(Argument("validation_split", "int", required=True, default=0.2,
                              toolTip="The number of training images vs test images"))

        self.app = zmlp.app_from_env()

        self.model = None
        self.labels = None
        self.base_dir = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.labels = self.app.models.get_label_counts(self.app_model)
        self.base_dir = tempfile.mkdtemp('pth-xfer-learning')
        self.check_labels()

    def process(self, frame):
        download_labeled_images(self.app_model,
                                "labels-standard",
                                self.base_dir,
                                ratio=self.arg_value('validation_split'))

        self.reactor.emit_status("Training model: {}".format(self.app_model.name))
        image_datasets, data_loaders = self.build_data_loaders()

        # Build the label list
        labels = image_datasets['train'].classes
        self.train_model(image_datasets, data_loaders)

        self.publish_model(labels)

    def plot_history(self, history, name):
        self.logger.info('Saving history plot.')
        acc = history['train_acc']
        val_acc = history['val_acc']

        loss = history['train_loss']
        val_loss = history['val_loss']

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

        torch.save(self.model, model_dir + '/model.pth')
        with open(model_dir + '/labels.txt', 'w') as fp:
            for label in labels:
                fp.write('{}\n'.format(label))

        # We don't do this for now, this script is a courtesy to a user
        # who might download the classifier
        # pth_dir = os.path.dirname(os.path.realpath(__file__))
        # shutil.copy2(os.path.join(pth_dir, "predict.py"), model_dir)

        mod = file_storage.models.save_model(model_dir,
                                             self.app_model,
                                             self.arg_value('deploy'))
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

    def train_model(self, image_datasets, data_loaders):
        """
        Build and train Pytorch model using the base model specified in the args.

        """
        dataset_sizes = {x: len(image_datasets[x]) for x in ['train', 'validate']}

        # Make a new model from the base ResNet18 model.
        self.model = self.get_base_model()
        num_ftrs = self.model.fc.in_features
        # Here the size of each output sample is set to 2.
        # Alternatively, it can be generalized to nn.Linear(num_ftrs, len(class_names)).
        self.model.fc = nn.Linear(num_ftrs, len(image_datasets['train'].classes))

        device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
        self.model = self.model.to(device)

        criterion = nn.CrossEntropyLoss()

        # Observe that all parameters are being optimized
        optimizer = optim.SGD(self.model.parameters(), lr=0.001, momentum=0.9)

        # Decay LR by a factor of 0.1 every 7 epochs
        scheduler = lr_scheduler.StepLR(optimizer, step_size=7, gamma=0.1)
        ###
        since = time.time()

        best_model_wts = copy.deepcopy(self.model.state_dict())
        best_acc = 0.0
        num_epochs = self.arg_value('epochs')

        # We build a history dict that kind of resembles the Tensorflow one
        # so we can plot later
        history = {'train_acc': [], 'train_loss': [], 'val_acc': [], 'val_loss': []}
        for epoch in range(num_epochs):
            self.reactor.emit_status('Epoch {}/{}'.format(epoch, num_epochs - 1))
            self.reactor.emit_status('-' * 10)

            # Each epoch has a training and validation phase
            for phase in ['train', 'validate']:
                if phase == 'train':
                    self.model.train()  # Set model to training mode
                else:
                    self.model.eval()  # Set model to evaluate mode

                running_loss = 0.0
                running_corrects = 0

                # Iterate over data.
                for inputs, labels in data_loaders[phase]:
                    inputs = inputs.to(device)
                    labels = labels.to(device)

                    # zero the parameter gradients
                    optimizer.zero_grad()

                    # forward
                    # track history if only in train
                    with torch.set_grad_enabled(phase == 'train'):
                        outputs = self.model(inputs)
                        _, preds = torch.max(outputs, 1)
                        loss = criterion(outputs, labels)

                        # backward + optimize only if in training phase
                        if phase == 'train':
                            loss.backward()
                            optimizer.step()

                    # statistics
                    running_loss += loss.item() * inputs.size(0)
                    running_corrects += torch.sum(preds == labels.data)
                if phase == 'train':
                    scheduler.step()

                epoch_loss = running_loss / dataset_sizes[phase]
                epoch_acc = running_corrects.double() / dataset_sizes[phase]

                if phase == 'train':
                    history['train_acc'].append(epoch_acc)
                    history['train_loss'].append(epoch_loss)
                else:
                    history['val_acc'].append(epoch_acc)
                    history['val_loss'].append(epoch_loss)

                self.reactor.emit_status('{} Loss: {:.4f} Acc: {:.4f}'.format(
                    phase, epoch_loss, epoch_acc))

                # deep copy the model
                if phase == 'validate' and epoch_acc > best_acc:
                    best_acc = epoch_acc
                    best_model_wts = copy.deepcopy(self.model.state_dict())

        time_elapsed = time.time() - since
        self.reactor.emit_status('Training complete in {:.0f}m {:.0f}s'.format(
            time_elapsed // 60, time_elapsed % 60))
        self.reactor.emit_status('Best val Acc: {:4f}'.format(best_acc))

        self.plot_history(history, "history")

        # load best model weights
        self.model.load_state_dict(best_model_wts)

    def build_data_loaders(self):
        """
        Build the pytorch DataLoader used to load in the the train
        and test images.

        Returns:
            dict: a dictionary of DataLoaders, "train" and "val".
        """

        data_transforms = {
            'train': transforms.Compose([
                transforms.RandomResizedCrop(224),
                transforms.RandomHorizontalFlip(),
                transforms.ToTensor(),
                transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225])
            ]),
            'validate': transforms.Compose([
                transforms.Resize(256),
                transforms.CenterCrop(224),
                transforms.ToTensor(),
                transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225])
            ]),
        }

        image_datasets = {x: datasets.ImageFolder(os.path.join(self.base_dir, x),
                                                  data_transforms[x])
                          for x in ['train', 'validate']}
        data_loaders = {x: torch.utils.data.DataLoader(image_datasets[x],
                                                       batch_size=4,
                                                       shuffle=True,
                                                       num_workers=4)
                        for x in ['train', 'validate']}

        return image_datasets, data_loaders

    @staticmethod
    def get_base_model():
        """
        Return the base ResNet18 model.

        Returns:
            Model: A pytorch model.

        Raises:
            ZmlpFatalProcessorException: If the model is not found

        """
        return models.resnet18(pretrained=True)
