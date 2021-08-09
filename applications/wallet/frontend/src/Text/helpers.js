export const capitalizeFirstLetter = ({ word }) => {
  return word.charAt(0).toUpperCase() + word.substring(1).toLowerCase()
}

// insert a space before each caps
export const decamelize = ({ word }) => {
  return word.replace(/([A-Z])/g, ' $1').trim()
}
