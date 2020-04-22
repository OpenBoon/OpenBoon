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
