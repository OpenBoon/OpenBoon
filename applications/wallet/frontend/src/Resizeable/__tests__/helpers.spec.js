import { calculateDelta } from '../helpers'

describe('<Resizeable /> helper', () => {
  describe('calculateDelta()', () => {
    it('should calculate', () => {
      const mockSet = jest.fn()

      calculateDelta({ width: 400, setWidth: mockSet })('', { deltaX: 10 })

      expect(mockSet).toHaveBeenCalledWith({ value: 390 })
    })
  })
})
