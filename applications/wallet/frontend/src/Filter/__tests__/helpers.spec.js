import { formatOptions, getOptions, getValues } from '../helpers'

const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

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

  describe('getOptions()', () => {
    it('should get "labelsExist" options', () => {
      expect(
        getOptions({
          filter: { datasetId: DATASET_ID, attribute: 'labels.pets' },
          fields: { labels: { [DATASET_ID]: ['labels', 'labelsExist'] } },
        }),
      ).toEqual(['labels', 'labelsExist'])
    })
  })

  describe('formatOptions()', () => {
    it('should format "similarity"', () => {
      expect(formatOptions({ option: 'similarity' })).toEqual(
        'similarity range',
      )
    })

    it('should format "labelsExist"', () => {
      expect(formatOptions({ option: 'labelsExist' })).toEqual('exists')
    })

    it('should format "label confidence"', () => {
      expect(formatOptions({ option: 'labelConfidence' })).toEqual('prediction')
    })

    it('should format "prediction count"', () => {
      expect(formatOptions({ option: 'predictionCount' })).toEqual('count')
    })
  })
})
