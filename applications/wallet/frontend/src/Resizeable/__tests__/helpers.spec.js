import { getFinalSize } from '../helpers'

const MIN_EXPANDED_SIZE = 300
const MIN_COLLAPSED_SIZE = 30

describe('<Resizeable /> helpers', () => {
  describe('getFinalSize', () => {
    it('should return minExpandedSize when dragging up towards startingSize', () => {
      expect(
        getFinalSize({
          startingSize: 150,
          newSize: 250,
          minExpandedSize: MIN_EXPANDED_SIZE,
          minCollapsedSize: MIN_COLLAPSED_SIZE,
        }),
      ).toEqual(MIN_EXPANDED_SIZE)
    })

    it('should return minCollapsedSize when dragging away from startingSize', () => {
      expect(
        getFinalSize({
          startingSize: 250,
          newSize: 50,
          minExpandedSize: MIN_EXPANDED_SIZE,
          minCollapsedSize: MIN_COLLAPSED_SIZE,
        }),
      ).toEqual(MIN_COLLAPSED_SIZE)
    })

    it('should return newSize if no startingSize', () => {
      expect(
        getFinalSize({
          startingSize: 0,
          newSize: 350,
          minExpandedSize: MIN_EXPANDED_SIZE,
          minCollapsedSize: MIN_COLLAPSED_SIZE,
        }),
      ).toEqual(350)
    })
  })
})
