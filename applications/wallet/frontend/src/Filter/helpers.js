export const formatOptions = ({ option }) => {
  if (option === 'similarity') {
    return 'similarity range'
  }

  if (option === 'labelConfidence') {
    return 'prediction'
  }

  return option
}
