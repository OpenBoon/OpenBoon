export const formatFullDate = ({ timestamp }) => {
  const formatOptions = {
    month: '2-digit',
    day: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    hourCycle: 'h23',
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

export const formatISODate = ({ date }) => date.toISOString().slice(0, 10)

export const formatPrettyDate = ({ timestamp }) => {
  const formatOptions = {
    month: '2-digit',
    day: '2-digit',
    year: 'numeric',
    hour: 'numeric',
    hourCycle: 'h23',
    minute: '2-digit',
    timeZoneName: 'short',
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

  const { year, month, day, hour, minute, timeZoneName } = partsIndex

  return `${year}-${month}-${day} ${hour}:${minute} ${timeZoneName}`
}

export const getDuration = ({ timeStarted, timeStopped, now }) => {
  const stopTime = timeStopped < 0 ? now : timeStopped
  return stopTime - timeStarted
}

export const formatDuration = ({ seconds }) => {
  const date = new Date(0)
  date.setSeconds(seconds)
  const isoString = date.toISOString().substr(11, 8)
  const parts = isoString.split(':')
  return `${parts[0]}h : ${parts[1]}m : ${parts[2]}s`
}
