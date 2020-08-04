const aggregate = {
  count: 5,
  results: {
    docCountErrorUpperBound: 0,
    sumOtherDocCount: 0,
    buckets: [
      { key: 'Ford', docCount: 5 },
      { key: 'Chevrolet', docCount: 4 },
      { key: 'Peugeot', docCount: 3 },
      { key: 'Audi', docCount: 2 },
      { key: 'Toyota', docCount: 1 },
    ],
  },
}

export default aggregate
