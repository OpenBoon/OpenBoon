import deepfilter from 'deep-filter'

export const getFilteredOptions = ({ options, searchString }) => {
  return deepfilter(options, (option, prop) => {
    if (!searchString) return true

    if (typeof prop === 'string' && typeof option === 'string') {
      return prop.toLowerCase().includes(searchString)
    }

    if (typeof option === 'object' && Object.keys(option).length === 0) {
      return false
    }

    return true
  })
}
