export const getCurrentPeriod = ({ date }) => {
  const month = date.toLocaleDateString('en-US', { month: 'long' })
  const day = date.toLocaleDateString('en-US', { day: 'numeric' })
  const year = date.toLocaleDateString('en-US', { year: 'numeric' })
  return `${month} 1–${day}, ${year}`
}
