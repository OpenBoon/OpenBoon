import { formatFullDate } from '../helpers'

describe('<Date /> helpers', () => {
  describe('formatFullDate()', () => {
    it('should return formatted date string', () => {
      const timestamp = 1573090717162
      const formattedString = '2019-11-07 01:38:37'
      expect(formatFullDate({ timestamp })).toEqual(formattedString)
    })
  })
})
