import { capitalizeFirstLetter } from '../Text/helpers'

export const formatFields = ({ fields, type, path = '' }) => {
  if (Array.isArray(fields)) {
    if (fields.includes(type)) {
      return path
    }

    return null
  }

  return Object.keys(fields).reduce((acc, field) => {
    const child = formatFields({
      fields: fields[field],
      type,
      path: path ? [path, field].join('.') : field,
    })

    if (!child || Object.keys(child).length === 0) return acc

    return {
      ...acc,
      [typeof child === 'string'
        ? field
        : capitalizeFirstLetter({ word: field })]: child,
    }
  }, {})
}
