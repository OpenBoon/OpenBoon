const modelTypes = {
  results: [
    {
      name: 'KNN_CLASSIFIER',
      label: 'K-Nearest Neighbors Classifier',
      description:
        'Classify images, documents and video clips using a KNN classifier.  This type of model can work great with just a single labeled example.If no labels are provided, the model automatically generates numbered groups of similar assets. These groups can be renamed and edited in subsequent training passes.',
      objective: 'Label Detection',
      provider: 'Boon AI',
      deployOnTrainingSet: true,
      minConcepts: 0,
      minExamples: 0,
    },
    {
      name: 'TF_CLASSIFIER',
      label: 'Tensorflow Transfer Learning Classifier',
      description:
        'Classify images or documents using a custom strained CNN deep learning algorithm.  This type of modelgenerates multiple predictions and can be trained to identify very specific features. The label detection classifier requires at least 2 concepts with 10 labeled images each.',
      objective: 'Label Detection',
      provider: 'Boon AI',
      deployOnTrainingSet: false,
      minConcepts: 2,
      minExamples: 10,
    },
    {
      name: 'FACE_RECOGNITION',
      label: 'Face Recognition',
      description:
        'Label faces detected by the boonai-face-detection module, and classify them with a KNN model.',
      objective: 'Face Recognition',
      provider: 'Boon AI',
      deployOnTrainingSet: true,
      minConcepts: 1,
      minExamples: 1,
    },
    {
      name: 'PYTORCH_MODEL_ARCHIVE',
      label:
        'A Pytorch Model Archive. image_classifier, image_segmenter, object_detector, or text_classifier.',
      description: 'Upload a pre-trained Pytorch Model Archive',
      objective: 'Label Detection',
      provider: 'Boon AI',
      deployOnTrainingSet: true,
      minConcepts: 0,
      minExamples: 0,
    },
  ],
}

export default modelTypes
