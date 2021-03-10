const modelTypes = {
  results: [
    {
      name: 'KNN_CLASSIFIER',
      label: 'K-Nearest Neighbors Classifier',
      description:
        'Classify images or documents using a KNN classifier.  This type of model generates a single prediction which can be used to quickly organize assets into general groups.The KNN classifier works with just a single image and label.',
      objective: 'Label Detection',
      provider: 'Zorroa',
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
      provider: 'Zorroa',
      deployOnTrainingSet: false,
      minConcepts: 2,
      minExamples: 10,
    },
    {
      name: 'TF_UPLOADED_CLASSIFIER',
      label: 'Imported Tensorflow Image Classifier',
      description: 'Upload a Tensorflow model to use for image classification.',
      objective: 'Label Detection',
      provider: 'Google',
      deployOnTrainingSet: true,
      minConcepts: 0,
      minExamples: 0,
    },
    {
      name: 'GCP_AUTOML_CLASSIFIER',
      label: 'Google AutoML Classifier',
      description: 'Utilize Google AutoML to train an image classifier.',
      objective: 'Label Detection',
      provider: 'Google',
      deployOnTrainingSet: true,
      minConcepts: 2,
      minExamples: 10,
    },
  ],
}

export default modelTypes
