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
