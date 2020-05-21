#!/usr/bin/env python

import logging
import argparse
import numpy as np
import random

from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Activation
from tensorflow.keras.optimizers import SGD
from tensorflow.keras.utils import to_categorical

import zmlp
from zmlpsdk import file_storage
from zmlp_train.utils.utils import get_labels, extract_model
from zmlp_train.utils.models import upload_model_directory

logging.basicConfig()


def load_data(app, app_model, simhash):
    train_x = []
    train_y = []
    test_x = []
    test_y = []

    PTEST = 0.3
    random.seed(42)

    app_model_name = app_model.name
    model_zip = file_storage.projects.localize_file(app_model.file_id)
    tmpfile_dir = extract_model(model_zip)
    class_names = get_labels(tmpfile_dir, app_model_name, "labels.txt")

    for a in app.assets.search():
        num_hash = []
        charhash = a.get_attr(simhash)

        for char in charhash:
            num_hash.append((ord(char)-65)/16.)

        if random.random() > PTEST:
            train_x.append(num_hash)
            train_y.append(app_model_name)
        else:
            test_x.append(num_hash)
            test_y.append(app_model_name)

    # Convert labels to an array of indices
    cats = list(set(train_y))
    train_y = [cats.index(i) for i in train_y]
    test_y = [cats.index(i) for i in test_y]

    # Make everything into numpy arrays
    train_x = np.array(train_x)
    train_y = to_categorical(train_y, len(class_names))
    test_x = np.array(test_x)
    test_y = to_categorical(test_y, len(class_names))

    return (train_x, train_y), (test_x, test_y), cats


def main():
    parser = argparse.ArgumentParser(prog="trainClassifier")
    parser.add_argument("-m", "--model_id", help="The model ID")
    parser.add_argument("-a", "--attr", default='similarity.resnet', help="Attribute to use as features.")
    parser.add_argument("-f", "--folder", default='', help="Folder containing subfolders specifying classes. This will be the name of the classifier.")
    parser.add_argument("-n", "--n_neurons", default='100', help="Number of neurons in the hidden layer.")
    args = parser.parse_args()

    logging.info('Looking for categories:')

    # get model info
    app = zmlp.app_from_env()
    app_model = app.models.get_model(args.model_id)
    model_dir = args.folder

    # load data
    (train_x, train_y), (test_x, test_y), class_names = \
        load_data(app, app_model, args.attr)

    # add layers
    model = Sequential()
    model.add(Dense(args.n_neurons, input_dim=train_x.shape[1], activation='relu'))
    model.add(Dense(len(class_names)))
    model.add(Activation("softmax"))

    # train
    sgd = SGD(lr=0.01)
    model.compile(loss="binary_crossentropy", optimizer=sgd, metrics=["accuracy"])
    model.fit(train_x, train_y, epochs=50, batch_size=128, verbose=1)

    # get scores
    scores = model.evaluate(test_x, test_y)
    logging.info('Results on the test set are %f accuracy and %f loss.' % (
        scores[1], scores[0]))

    # save model and labels
    model.save(model_dir + '.hd5')
    with open(model_dir + '_labels.txt', 'w') as classes_file:
        for label in class_names:
            print(label, file=classes_file)

    # zip and publish
    upload_model_directory(model_dir, app_model.file_id)
    app.models.publish_model(app_model)

    logging.info('Model saved as ' + model_dir + '.hd5')
    logging.info('Labels are in ' + model_dir + '_labels.txt')
    logging.info('Use these two files with the KerasClassifier processor')


if __name__ == '__main__':
    main()
