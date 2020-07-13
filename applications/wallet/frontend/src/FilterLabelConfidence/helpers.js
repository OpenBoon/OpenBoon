export const formatRange = ({ min, max, labels }) => {
  if (labels.length === 0) return 'N/A'

  if (min === 0 && max === 1) return 'All'

  if (min === 0) return `< ${max.toFixed(2)}`

  if (max === 1) return `> ${min.toFixed(2)}`

  return `${min.toFixed(2)} < conf < ${max.toFixed(2)}`
}
