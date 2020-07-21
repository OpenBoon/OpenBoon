const modelTypes = {
  results: [
    {
      name: 'ZVI_KNN_CLASSIFIER',
      description:
        'Classify images or documents using a KNN classifier.  This type of model generates a single prediction which can be used to quickly organize assets into general groups.The KNN classifier works with just a single image and label.',
      mlType: 'Label Detection',
      provider: 'Zorroa',
      runOnTrainingSet: true,
    },
    {
      name: 'ZVI_LABEL_DETECTION',
      description:
        'Classify images or documents using a custom strained CNN deep learning algorithm.  This type of modelgenerates multiple predictions and can be trained to identify very specific features. The label detection classifier requires at least 2 concepts with 10 labeled images each.',
      mlType: 'Label Detection',
      provider: 'Zorroa',
      runOnTrainingSet: false,
    },
    {
      name: 'ZVI_FACE_RECOGNITION',
      description:
        'Relabel existing ZVI faces using a KNN Face Recognition model.',
      mlType: 'Face Recognition',
      provider: 'Zorroa',
      runOnTrainingSet: true,
    },
    {
      name: 'GCP_LABEL_DETECTION',
      description: 'Utilize Google AutoML to train an image classifier.',
      mlType: 'Label Detection',
      provider: 'Google',
      runOnTrainingSet: true,
    },
  ],
}

export default modelTypes
