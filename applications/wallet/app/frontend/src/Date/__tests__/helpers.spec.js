import { formatFullDate, getDuration } from '../helpers'

describe('formatFullDate()', () => {
  it('should return formatted date string', () => {
    const timestamp = 1573090717162
    const formattedString = '2019-11-07 01:38:37'
    expect(formatFullDate({ timestamp })).toEqual(formattedString)
  })
})

describe('getDuration()', () => {
  it('should return formatted duration string', () => {
    const timeStarted = 1564699941318
    const timeEnded = 1565211006581
    const duration = {
      days: 5,
      hours: 21,
      minutes: 57,
      seconds: 45,
    }
    expect(getDuration({ timeStarted, timeEnded })).toEqual(duration)
  })
})
