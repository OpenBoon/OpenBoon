export const formatSeconds = ({ seconds }) => {
  const ISOString = new Date(seconds * 1000).toISOString()

  // has double digit hours
  if (seconds > 36000) return ISOString.substr(11, 8)

  // has single digit hours
  if (seconds > 3600) return ISOString.substr(12, 7)

  // has double digit minutes
  if (seconds > 600) return ISOString.substr(14, 5)

  // has single digit minutes or less than 1 minute
  return ISOString.substr(15, 4)
}
