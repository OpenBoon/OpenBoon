import { getTimeEnded } from '../helpers'

describe('getTimeEnded()', () => {
  describe('when state is In Progress', () => {
    it('should return currentTime', () => {
      const state = 'In Progress'
      const currentTime = 1565211006590
      const timeUpdated = 1565211006580
      getTimeEnded({ state, timeUpdated })
      expect(getTimeEnded({ state, currentTime, timeUpdated })).toEqual(
        currentTime,
      )
    })
  })

  describe('when state is not In Progress', () => {
    it('should return timeUpdated', () => {
      const state = 'Canceled'
      const currentTime = 1565211006590
      const timeUpdated = 1565211006580
      getTimeEnded({ state, timeUpdated })
      expect(getTimeEnded({ state, currentTime, timeUpdated })).toEqual(
        timeUpdated,
      )
    })
  })
})
