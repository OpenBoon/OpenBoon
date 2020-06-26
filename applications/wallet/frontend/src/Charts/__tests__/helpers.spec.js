import { setAllLayouts } from '../helpers'

describe('<Charts /> helpers', () => {
  describe('setAllLayouts()', () => {
    it('should set min dimensions', () => {
      const mockFn = jest.fn()

      setAllLayouts({ setLayouts: mockFn })(null, {
        lg: [{}],
      })

      expect(mockFn).toHaveBeenCalledWith({
        value: {
          lg: [{ w: 4, minW: 4, h: 4, minH: 4 }],
        },
      })
    })

    it('should not ovveride set dimensions', () => {
      const mockFn = jest.fn()

      setAllLayouts({ setLayouts: mockFn })(null, {
        lg: [{ w: 4, minW: 4, h: 6, minH: 4 }],
      })

      expect(mockFn).toHaveBeenCalledWith({
        value: {
          lg: [{ w: 4, minW: 4, h: 6, minH: 4 }],
        },
      })
    })
  })
})
