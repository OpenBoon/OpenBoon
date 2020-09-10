import { getFinalSize } from '../helpers'

const MIN_EXPANDED_SIZE = 300
const MIN_COLLAPSED_SIZE = 30

describe('<Resizeable /> helpers', () => {
  describe('getFinalSize', () => {
    it('should return minExpandedSize when dragging up towards startingAxis', () => {
      expect(
        getFinalSize({
          startingAxis: 150,
          sizeCalculation: 250,
          minExpandedSize: MIN_EXPANDED_SIZE,
          minCollapsedSize: MIN_COLLAPSED_SIZE,
        }),
      ).toEqual(MIN_EXPANDED_SIZE)
    })

    it('should return minCollapsedSize when dragging away from startingAxis', () => {
      expect(
        getFinalSize({
          startingAxis: 250,
          sizeCalculation: 50,
          minExpandedSize: MIN_EXPANDED_SIZE,
          minCollapsedSize: MIN_COLLAPSED_SIZE,
        }),
      ).toEqual(MIN_COLLAPSED_SIZE)
    })

    it('should return sizeCalculation if no startingAxis', () => {
      expect(
        getFinalSize({
          startingAxis: 0,
          sizeCalculation: 350,
          minExpandedSize: MIN_EXPANDED_SIZE,
          minCollapsedSize: MIN_COLLAPSED_SIZE,
        }),
      ).toEqual(350)
    })
  })
})
