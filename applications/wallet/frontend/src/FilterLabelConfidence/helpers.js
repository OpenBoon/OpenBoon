export const formatRange = ({ min, max }) => {
  if (min === 0 && max === 1) return 'All'

  if (min === 0) return `< ${max}`

  if (max === 1) return `> ${min}`

  return `${min} < conf < ${max}`
}
