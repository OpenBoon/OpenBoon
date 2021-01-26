export const formatUsage = ({ number }) => {
  if (number < 0) {
    return 'unavailable**'
  }

  return Math.ceil(number).toLocaleString()
}
