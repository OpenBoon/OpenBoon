# Classifiers plugin

This plugin contains the NeuralNetClassifierProcessor.

This processor deploys a Keras-trained classifier neural network.
It is assumed that the neural network takes a feature vector, or similarity hash, as input.

The neural network is described by two files:

\<NAME>.hd4: Keras weights as saved by that library.
<br>
\<NAME>_labels.txt: A text file that contains the following:
- One line specifying the hash to use as input
- One line per label the classifier was trained on

\<NAME> is the name of the classifier, as provided to the Tools/ML/trainClassifier.py script
when the classifier was trained.

See the files under "models" in this directory for an example.