import { bytesToSize } from '../Bytes/helpers'
import { formatPrettyDate } from '../Date/helpers'

export const capitalizeFirstLetter = ({ word }) =>
  word.charAt(0).toUpperCase() + word.substring(1)

export const formatDisplayName = ({ name }) =>
  name
    // insert a space before all caps
    .replace(/([A-Z])/g, ' $1')
    .replace('_', ' ')
    .split(' ')
    .flatMap((word) => {
      if (word.toLowerCase() === 'url') return 'URL'

      if (word.toLowerCase() === 'id') return 'ID'

      if (
        word.toLowerCase().includes('file') &&
        word.toLowerCase() !== 'files'
      ) {
        // Separate and capitalize word after File
        return ['File', word.charAt(4).toUpperCase() + word.substring(5)]
      }

      // Capitalize first letter
      return capitalizeFirstLetter({ word })
    })
    .join(' ')

export const formatDisplayValue = ({ name, value }) => {
  if (name.includes('size')) {
    return bytesToSize({ bytes: value })
  }

  // https://regex101.com/r/JrKX84/1
  // Will match "2020-04-30T01:48:20" and more but no less
  const ISO_8601 = /^\d{4}(-\d\d(-\d\d(T\d\d:\d\d(:\d\d)(\.\d+)?(([+-]\d\d:\d\d)|Z)?)))$/i

  if (value.toString().match(ISO_8601)) {
    return formatPrettyDate({ timestamp: value })
  }

  if (value.length > 300) {
    return `${value.slice(0, 300)}...`
  }

  return value
}
