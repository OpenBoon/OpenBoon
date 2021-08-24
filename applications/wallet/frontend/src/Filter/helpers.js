export const getValues = ({ type, ids }) => {
  switch (type) {
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

export const formatOptions = ({ option }) => {
  if (option === 'similarity') {
    return 'similarity range'
  }

  if (option === 'labelConfidence') {
    return 'prediction'
  }

  if (option === 'predictionCount') {
    return 'count'
  }

  return option
}
