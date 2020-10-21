const timelines = [
  {
    timeline: 'gcp-video-text-detection',
    tracks: [
      {
        track: 'Gh st',
        hits: [
          { start: 0, stop: 2, highlight: false },
          { start: 4, stop: 5, highlight: false },
          { start: 10, stop: 12, highlight: false },
        ],
      },
      {
        track: 'Lemon',
        hits: [{ start: 6, stop: 7, highlight: true }],
      },
      {
        track: 'Busters',
        hits: [
          { start: 6, stop: 7, highlight: false },
          { start: 13, stop: 14, highlight: false },
          { start: 15, stop: 15, highlight: false },
        ],
      },
    ],
  },
  {
    timeline: 'gcp-video-label-detection',
    tracks: [
      {
        track: 'Label 1',
        hits: [
          { start: 1, stop: 2, highlight: false },
          { start: 3, stop: 6, highlight: false },
          { start: 11, stop: 15, highlight: false },
        ],
      },
      {
        track: 'Label 2 Plus More Text to Make A Long Label String',
        hits: [
          { start: 7, stop: 10, highlight: false },
          { start: 16, stop: 17, highlight: false },
        ],
      },
    ],
  },
  {
    timeline: 'gcp-video-object-detection',
    tracks: [
      {
        track: 'Object 1',
        hits: [
          { start: 0, stop: 2, highlight: false },
          { start: 4, stop: 5, highlight: false },
          { start: 10, stop: 12, highlight: false },
        ],
      },
      {
        track: 'Object 2',
        hits: [
          { start: 0, stop: 2, highlight: false },
          { start: 4, stop: 5, highlight: false },
          { start: 10, stop: 12, highlight: false },
        ],
      },
      {
        track: 'Object 3',
        hits: [
          { start: 0, stop: 2, highlight: false },
          { start: 4, stop: 5, highlight: false },
          { start: 10, stop: 12, highlight: false },
        ],
      },
    ],
  },
  {
    timeline: 'gcp-video-logo-detection',
    tracks: [
      {
        track: 'Logo 1',
        hits: [
          { start: 0, stop: 2, highlight: false },
          { start: 4, stop: 5, highlight: false },
          { start: 10, stop: 12, highlight: false },
        ],
      },
      {
        track: 'Logo 2',
        hits: [
          { start: 0, stop: 2, highlight: false },
          { start: 4, stop: 5, highlight: false },
          { start: 10, stop: 12, highlight: false },
        ],
      },
      {
        track: 'Logo 3',
        hits: [
          { start: 0, stop: 2, highlight: false },
          { start: 4, stop: 5, highlight: false },
          { start: 10, stop: 12, highlight: false },
        ],
      },
    ],
  },
]

export default timelines
