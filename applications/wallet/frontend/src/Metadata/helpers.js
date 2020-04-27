import { bytesToSize } from '../Bytes/helpers'
import { formatPrettyDate } from '../Date/helpers'

export const formatDisplayName = ({ name }) =>
  name
    // insert a space before all caps
    .replace(/([A-Z])/g, ' $1')
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
      return word.charAt(0).toUpperCase() + word.substring(1)
    })
    .join(' ')

export const formatDisplayValue = ({ name, value }) => {
  if (name.includes('size')) {
    return bytesToSize({ bytes: value })
  }

  if (name.toLowerCase().includes('time')) {
    return formatPrettyDate({ timestamp: value })
  }

  if (value.length > 300) {
    return `${value.slice(0, 300)}...`
  }

  return value
}
