import { getMetadata } from '../helpers'

describe('<MetadataCues /> helpers', () => {
  describe('getMetadata()', () => {
    it('should get the metadata', () => {
      expect(
        getMetadata({
          target: {
            activeCues: [
              {
                text: JSON.stringify({
                  timeline: 'gcp-label-detection',
                  score: 0.91,
                  content: ['cheese', 'mozzarella'],
                }),
              },
              {
                text: JSON.stringify({
                  timeline: 'gcp-label-detection',
                  score: 0.82,
                  content: ['pepperoni'],
                }),
              },
              {
                text: JSON.stringify({
                  timeline: 'gcp-object-detection',
                  score: 0.73,
                  content: ['food'],
                }),
              },
            ],
          },
        }),
      ).toEqual({
        'gcp-label-detection': [
          { label: 'cheese, mozzarella', score: 0.91 },
          { label: 'pepperoni', score: 0.82 },
        ],
        'gcp-object-detection': [{ label: 'food', score: 0.73 }],
      })
    })

    it('should clear the metadata', () => {
      expect(
        getMetadata({
          target: {
            label: 'gcp-label-detection',
            activeCues: [],
          },
        }),
      ).toEqual({
        'gcp-label-detection': [],
      })
    })
  })
})
