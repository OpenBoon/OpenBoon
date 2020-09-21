const detections = [
  {
    name: 'gcp-video-explicit-detection',
    predictions: [
      { label: 'Gh st', count: 3 },
      { label: 'Busters', count: 3 },
    ],
  },
  {
    name: 'gcp-video-label-detection',
    predictions: [
      { label: 'Label 1', count: 3 },
      { label: 'Label 2', count: 6 },
      { label: 'Label 3 Plus More Text to Make A Long Label String', count: 9 },
    ],
  },
  {
    name: 'gcp-video-logo-detection',
    predictions: [
      { label: 'Logo 1', count: 1 },
      { label: 'Logo 2', count: 3 },
      { label: 'Logo 3', count: 5 },
    ],
  },
  {
    name: 'gcp-video-object-detection',
    predictions: [
      { label: 'Object 1', count: 3 },
      { label: 'Object 2', count: 4 },
      { label: 'Object 3', count: 4 },
    ],
  },
  {
    name: 'gcp-video-text-detection',
    predictions: [
      { label: 'Text 1', count: 2 },
      { label: 'Text 2', count: 4 },
      { label: 'Text 3', count: 6 },
      { label: 'Text 4', count: 8 },
    ],
  },
]

export default detections
