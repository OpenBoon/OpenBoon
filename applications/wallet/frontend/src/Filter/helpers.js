export const getValues = ({ type, ids }) => {
  switch (type) {
    case 'exists':
    case 'labelsExist': {
      return { exists: true }
    }

    case 'similarity': {
      if (ids) {
        return { ids }
      }

      return {}
    }

    case 'limit': {
      return { maxAssets: 10_000 }
    }

    default: {
      return {}
    }
  }
}

export const getOptions = ({ filter, fields }) => {
  if (filter.datasetId && filter.attribute.split('.')[0] === 'labels') {
    return fields.labels[filter.datasetId]
  }

  const options =
    filter.attribute.split('.').reduce((acc, cur) => acc && acc[cur], fields) ||
    []

  return options
}

export const formatOptions = ({ option }) => {
  if (option === 'similarity') {
    return 'similarity range'
  }

  if (option === 'labelsExist') {
    return 'exists'
  }

  if (option === 'labelConfidence') {
    return 'prediction'
  }

  if (option === 'predictionCount') {
    return 'count'
  }

  return option
}
