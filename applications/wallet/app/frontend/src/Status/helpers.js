export const getStatusLabel = ({ jobStatus }) => {
  const tokens = [...jobStatus.matchAll(/[A-Z][a-z]*(?=[A-Z])*/g)].map(
    match => match[0],
  )
  const label = tokens.reduce((reducedString, token) => {
    const updatedString = `${reducedString + token} `
    return updatedString
  }, '')

  return label
}
