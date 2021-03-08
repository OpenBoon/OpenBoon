import { formatFullDate, formatPrettyDate, getDuration } from '../helpers'

const TIME_STARTED = 1583451662066
const TIME_STOPPED = 1583451752821
const TIME_NOW = 1587969769607

describe('<Date /> helpers', () => {
  describe('formatFullDate()', () => {
    it('should return formatted date string', () => {
      const timestamp = 1573090717162
      const formattedString = '2019-11-07 01:38:37'
      expect(formatFullDate({ timestamp })).toEqual(formattedString)
    })
  })

  describe('formatPrettyDate()', () => {
    it('should return formatted date string', () => {
      const timestamp = '2020-04-10T00:27:25.526192Z'
      const formattedString = '2020-04-10 00:27 UTC'
      expect(formatPrettyDate({ timestamp })).toEqual(formattedString)
    })
  })

  describe('getDuration()', () => {
    it('should return properly when timeStopped is less than 0', () => {
      const duration = getDuration({
        timeStarted: TIME_STARTED,
        timeStopped: -1,
        now: TIME_NOW,
      })

      expect(duration).toEqual(TIME_NOW - TIME_STARTED)
    })
    it('should return properly when timeStopped is greater than 0', () => {
      const duration = getDuration({
        timeStarted: TIME_STARTED,
        timeStopped: TIME_STOPPED,
        now: TIME_NOW,
      })

      expect(duration).toEqual(TIME_STOPPED - TIME_STARTED)
    })
  })
})
