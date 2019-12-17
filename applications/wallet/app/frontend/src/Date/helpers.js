export const formatFullDate = ({ timestamp }) => {
  const formatOptions = {
    month: '2-digit',
    day: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    hour12: false,
    minute: '2-digit',
    second: '2-digit',
  }

  const partsIndex = new Intl.DateTimeFormat('en-US', formatOptions)
    .formatToParts(new Date(timestamp))
    .reduce((accumulator, part) => {
      const { type, value } = part
      if (type !== 'literal') {
        accumulator[type] = value
      }
      return accumulator
    }, {})

  const { year, month, day, hour, minute, second } = partsIndex

  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

export const calculateDuration = ({ timestamp }) => {
  const currentTime = Date.now()
  const timeStarted = timestamp
  const durationInSeconds = Math.floor((currentTime - timeStarted) / 1000)
  const durationInMinutes = Math.floor(durationInSeconds / 60)
  const durationInHours = Math.floor(durationInMinutes / 60)
  const durationInDays = Math.floor(durationInHours / 24)

  const seconds = durationInSeconds % 60
  const minutes = durationInMinutes % 60
  const hours = durationInHours % 24
  const days = durationInDays

  return {
    days,
    hours,
    minutes,
    seconds,
  }
}
