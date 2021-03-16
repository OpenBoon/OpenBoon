import { formatOptions } from '../helpers'

describe('<Filter /> helpers', () => {
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
