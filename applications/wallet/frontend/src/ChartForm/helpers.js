export const formatFields = ({ fields, type }) => {
  if (Array.isArray(fields)) {
    if (fields.includes(type)) {
      return fields
    }

    return null
  }

  return Object.keys(fields).reduce((acc, field) => {
    const child = formatFields({ fields: fields[field], type })

    if (!child || Object.keys(child).length === 0) return acc

    return { ...acc, [field]: child }
  }, {})
}
