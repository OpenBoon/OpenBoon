import { formatOptions } from '../helpers'

describe('<Filter /> helpers', () => {
  describe('formatOptions()', () => {
    it('should format "similarity"', () => {
      expect(formatOptions({ option: 'similarity' })).toEqual(
        'similarity range',
      )
    })
  })
})
