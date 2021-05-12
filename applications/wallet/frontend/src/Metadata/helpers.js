import deepfilter from 'deep-filter'

import { bytesToSize } from '../Bytes/helpers'
import { formatPrettyDate } from '../Date/helpers'
import { capitalizeFirstLetter } from '../Text/helpers'

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

      return capitalizeFirstLetter({ word })
    })
    .join(' ')

export const formatDisplayValue = ({ name, value }) => {
  if (name.includes('size')) {
    return bytesToSize({ bytes: value })
  }

  // https://regex101.com/r/JrKX84/1
  // Will match "2020-04-30T01:48:20" and more but no less
  const ISO_8601 =
    /^\d{4}(-\d\d(-\d\d(T\d\d:\d\d(:\d\d)(\.\d+)?(([+-]\d\d:\d\d)|Z)?)))$/i

  if (value.toString().match(ISO_8601)) {
    return formatPrettyDate({ timestamp: value })
  }

  if (value.length > 300) {
    return `${value.slice(0, 300)}...`
  }

  return value
}

export const filter = ({ metadata, searchString }) => {
  const filteredMetadata = deepfilter(metadata, (value, prop) => {
    if (!searchString) return true

    // Ignore first level section (analysis, files, media, etc.)
    if (Object.keys(metadata).includes(prop)) return true

    let regex

    try {
      regex = new RegExp(searchString, 'img')
    } catch (error) {
      regex = new RegExp('')
    }

    // Special case to filter processors
    if (
      typeof prop === 'number' &&
      typeof value === 'object' &&
      typeof value.processor === 'string'
    ) {
      return regex.test(value.processor)
    }

    // Filter entries that are an object because it means they
    // are a section a.k.a. a module
    if (
      typeof prop === 'string' &&
      typeof value === 'object' &&
      !Array.isArray(value)
    ) {
      return regex.test(prop)
    }

    return true
  })

  return filteredMetadata
}
