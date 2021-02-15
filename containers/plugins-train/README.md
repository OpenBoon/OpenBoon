# Training 
Contains trainers to be used for training models in Boon AI

# Summary of Trainers

- `Facial Recognition`
    - Trains the facial recognition model to better recognize faces within an image/frame given new or updated labels
- `KNN Label Detection`
    - Trains a KNN classifier for better label detection given a single asset's similarity hash and labels
- `Perceptron Label Detector`
    - Trains an MLP (multi-layer perceptron) for image classification
- `Tensorflow (TF) Transfer Learning (TL) Trainers`
    - These trainers are used for classification using TL from one of the following TF models
        - K-means
        - ResNet50
        - VGG16
        - MobileNet2
        - KNN