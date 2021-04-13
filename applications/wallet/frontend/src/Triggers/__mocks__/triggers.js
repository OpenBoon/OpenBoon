const triggers = {
  count: 2,
  next: null,
  previous: null,
  results: [
    {
      id: 1,
      name: 'ASSET_ANALYZED',
      displayName: 'Asset Analyzed',
      description: 'Asset is added to the Boon AI with initial analysis.',
    },
    {
      id: 2,
      name: 'ASSET_MODIFIED',
      displayName: 'Asset Modified',
      description:
        'Asset is modified through additional analysis or manual editing.',
    },
  ],
}

export default triggers
