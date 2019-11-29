const DateComponent = ({ date }) => {
  const dateObj = new Date(date)
  let month = dateObj.getMonth() + 1
  if (month < 10) {
    month = `0${month}`
  }
  const day = dateObj.getDate()
  const year = dateObj.getFullYear()

  const hour = dateObj.getHours()
  const minutes = dateObj.getMinutes()
  let seconds = dateObj.getSeconds()
  if (seconds < 10) {
    seconds = `0${seconds}`
  }

  return `${month}-${day}-${year} ${hour}:${minutes}:${seconds}`
}

export default DateComponent
