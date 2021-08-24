import { formatOptions, getValues } from '../helpers'

describe('<Filter /> helpers', () => {
  describe('getValues()', () => {
    it('should return "Similarity" ids', () => {
      expect(getValues({ type: 'similarity', ids: [1, 2, 3] })).toEqual({
        ids: [1, 2, 3],
      })
    })

    it('should ignore empty "Similarity"', () => {
      expect(getValues({ type: 'similarity' })).toEqual({})
    })

    it('should return "Limit" values', () => {
      expect(getValues({ type: 'limit' })).toEqual({ maxAssets: 10_000 })
    })
  })

  describe('formatOptions()', () => {
    it('should format "similarity"', () => {
      expect(formatOptions({ option: 'similarity' })).toEqual(
        'similarity range',
      )
    })

    it('should format "label confidence"', () => {
      expect(formatOptions({ option: 'labelConfidence' })).toEqual('prediction')
    })

    it('should format "prediction count"', () => {
      expect(formatOptions({ option: 'predictionCount' })).toEqual('count')
    })
  })
})
