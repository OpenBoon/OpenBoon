const fields = {
  analysis: {
    boonai: { tinyProxy: ['facet', 'text', 'exists'] },
    'boonai-image-similarity': { simhash: ['similarity', 'exists'] },
    'boonai-object-detection': { predictions: [], count: ['range', 'exists'] },
    'boonai-face-detection': ['labelConfidence', 'exists'],
  },
  aux: ['exists'],
  clip: {
    length: ['range', 'exists'],
    pile: ['facet', 'text', 'exists'],
    sourceAssetId: ['facet', 'text', 'exists'],
    start: ['range', 'exists'],
    stop: ['range', 'exists'],
    timeline: ['facet', 'text', 'exists'],
    type: ['facet', 'text', 'exists'],
  },
  datasets: ['exists'],
  files: ['exists'],
  location: {
    city: ['facet', 'text', 'exists'],
    code: ['facet', 'text', 'exists'],
    country: ['facet', 'text', 'exists'],
    point: ['exists'],
  },
  media: {
    aspect: ['range', 'exists'],
    author: ['facet', 'text', 'exists'],
    content: ['facet', 'text', 'exists'],
    description: ['facet', 'text', 'exists'],
    height: ['range', 'exists'],
    keywords: ['facet', 'text', 'exists'],
    length: ['range', 'exists'],
    orientation: ['facet', 'text', 'exists'],
    timeCreated: ['exists'],
    title: ['facet', 'text', 'exists'],
    type: ['facet', 'text', 'exists'],
    width: ['range', 'exists'],
  },
  metrics: { pipeline: ['exists'] },
  source: {
    checksum: ['range', 'exists'],
    extension: ['facet', 'text', 'exists'],
    filename: ['facet', 'text', 'exists'],
    filesize: ['range', 'exists'],
    mimetype: ['facet', 'text', 'exists'],
    path: ['facet', 'text', 'exists'],
  },
  system: {
    dataSourceId: ['facet', 'text', 'exists'],
    jobId: ['facet', 'text', 'exists'],
    projectId: ['facet', 'text', 'exists'],
    state: ['facet', 'text', 'exists'],
    taskId: ['facet', 'text', 'exists'],
    timeCreated: ['exists', 'date'],
    timeModified: ['exists'],
  },
  tmp: ['exists'],
}

export default fields
