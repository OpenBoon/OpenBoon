import { formatRange } from '../helpers'

describe('<FilterLabelConfidence /> helpers', () => {
  describe('formatRange()', () => {
    it('should return N/A', () => {
      expect(formatRange({ min: 0, max: 1, labels: [] })).toEqual('N/A')
    })

    it('should return all', () => {
      expect(formatRange({ min: 0, max: 1, labels: ['cat'] })).toEqual('All')
    })

    it('should return min', () => {
      expect(formatRange({ min: 0.2, max: 1, labels: ['cat'] })).toEqual(
        '> 0.20',
      )
    })

    it('should return max', () => {
      expect(formatRange({ min: 0, max: 0.8, labels: ['cat'] })).toEqual(
        '< 0.80',
      )
    })

    it('should return range', () => {
      expect(formatRange({ min: 0.2, max: 0.8, labels: ['cat'] })).toEqual(
        '0.20 < conf < 0.80',
      )
    })
  })
})
