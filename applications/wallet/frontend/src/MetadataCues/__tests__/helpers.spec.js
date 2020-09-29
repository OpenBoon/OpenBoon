import { getMetadata } from '../helpers'

describe('<MetadataCues /> helpers', () => {
  describe('getMetadata()', () => {
    it('should get the metadata', () => {
      expect(
        getMetadata({
          target: {
            activeCues: {
              0: {
                text: JSON.stringify({
                  timeline: 'gcp-label-detection',
                  score: 0.91,
                  track: 'cheese',
                }),
              },
              1: {
                text: JSON.stringify({
                  timeline: 'gcp-label-detection',
                  score: 0.82,
                  track: 'pepperoni',
                }),
              },
              2: {
                text: JSON.stringify({
                  timeline: 'gcp-object-detection',
                  score: 0.73,
                  track: 'food',
                }),
              },
            },
          },
        }),
      ).toEqual({
        'gcp-label-detection': [
          { label: 'cheese', score: 0.91 },
          { label: 'pepperoni', score: 0.82 },
        ],
        'gcp-object-detection': [{ label: 'food', score: 0.73 }],
      })
    })
  })
})
