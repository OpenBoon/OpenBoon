import { formatTitle } from '../helpers'

describe('<ChartHistogram /> helpers', () => {
  describe('formatTitle()', () => {
    it('should handle a single value', () => {
      expect(
        formatTitle({ buckets: [{ docCount: 1, key: 0.123 }], index: 0 }),
      ).toEqual('1 (0.12)')
    })
  })
})
