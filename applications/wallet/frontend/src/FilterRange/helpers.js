import { bytesToSize } from '../Bytes/helpers'

export const formatValue = ({ attribute, value }) => {
  if (attribute.includes('size')) {
    return bytesToSize({ bytes: value })
  }

  // Will always return 2 decimals at most, only if necessary
  return Math.round((value + Number.EPSILON) * 100) / 100
}
