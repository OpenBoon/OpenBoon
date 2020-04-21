import { getDuration } from '../helpers'

const TIME_STARTED = 1583451662066
const TIME_STOPPED = 1583451752821
const TIME_NOW = 1587969769607

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
