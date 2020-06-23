export const formatFields = ({ fields, type }) => {
  if (Array.isArray(fields)) {
    if (fields.includes(type.toLowerCase())) {
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

export const getCountList = ({ count }) => {
  if (count === 0) {
    return []
  }

  return [count, ...getCountList({ count: count - 1 })]
}
