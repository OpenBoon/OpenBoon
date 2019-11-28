const DateComponent = ({ date }) => {
  let month = date.getMonth() + 1
  if (month < 10) {
    month = `0${month}`
  }
  const day = date.getDate()
  const year = date.getFullYear()

  const hour = date.getHours()
  const minutes = date.getMinutes()
  let seconds = date.getSeconds()
  if (seconds < 10) {
    seconds = `0${seconds}`
  }

  return `${month}-${day}-${year} ${hour}:${minutes}:${seconds}`
}

export default DateComponent
