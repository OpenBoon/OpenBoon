const formatOptions = {
  month: '2-digit',
  day: '2-digit',
  year: 'numeric',
}

export const formatLocaleDate = ({ date }) => {
  const locale = navigator.langauge
  const localeDate = new Intl.DateTimeFormat(locale, formatOptions).format(date)

  return localeDate
}

export const formatDateWithDashes = ({ date }) => {
  const { year, month, day } = new Intl.DateTimeFormat('en-US', formatOptions)
    .formatToParts(date)
    .reduce((accumulator, part) => {
      const { type, value } = part
      if (type !== 'literal') {
        accumulator[type] = value
      }
      return accumulator
    }, {})

  return `${year}-${month}-${day}`
}

export const parseDate = ({ date }) => {
  if (!date) return ''

  // slice full ISO8601 values
  const [year, month, day] = date.slice(0, 10).split('-')

  return new Date(year, month - 1, day)
}

export const getMinMaxFix = ({ domainMin, domainMax }) => {
  if (
    domainMin &&
    domainMax &&
    domainMin.toISOString() === domainMax.toISOString()
  ) {
    return 1
  }

  return 0
}
