const detections = [
  {
    name: 'gcp-video-explicit-detection',
    predictions: [
      {
        label: 'Gh st',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
      {
        label: 'Busters',
        hits: [
          { start: 6, stop: 7 },
          { start: 13, stop: 14 },
          { start: 15, stop: 15 },
        ],
      },
    ],
  },
  {
    name: 'gcp-video-label-detection',
    predictions: [
      {
        label: 'Label 1',
        hits: [
          { start: 1, stop: 2 },
          { start: 3, stop: 6 },
          { start: 11, stop: 15 },
        ],
      },
      {
        label: 'Label 2 Plus More Text to Make A Long Label String',
        hits: [
          { start: 7, stop: 10 },
          { start: 16, stop: 17 },
        ],
      },
    ],
  },
  {
    name: 'gcp-video-logo-detection',
    predictions: [
      {
        label: 'Logo 1',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
      {
        label: 'Logo 2',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
      {
        label: 'Logo 3',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
    ],
  },
  {
    name: 'gcp-video-object-detection',
    predictions: [
      {
        label: 'Object 1',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
      {
        label: 'Object 2',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
      {
        label: 'Object 3',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
    ],
  },
  {
    name: 'gcp-video-text-detection',
    predictions: [
      {
        label: 'Text 1',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
      {
        label: 'Text 2',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
      {
        label: 'Text 3',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
      {
        label: 'Text 4',
        hits: [
          { start: 0, stop: 2 },
          { start: 4, stop: 5 },
          { start: 10, stop: 12 },
        ],
      },
    ],
  },
]

export default detections
