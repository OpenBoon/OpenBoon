import { setAllLayouts } from '../helpers'

describe('<Charts /> helpers', () => {
  describe('setAllLayouts()', () => {
    it('should set min dimensions', () => {
      const mockFn = jest.fn()

      setAllLayouts({
        charts: [{ id: 'abc', type: 'facet' }],
        setLayouts: mockFn,
      })(null, {
        lg: [{ i: 'abc' }],
      })

      expect(mockFn).toHaveBeenCalledWith({
        value: {
          lg: [{ i: 'abc', w: 4, minW: 4, h: 5, minH: 5 }],
        },
      })
    })

    it('should not ovveride set dimensions', () => {
      const mockFn = jest.fn()

      setAllLayouts({ charts: [], setLayouts: mockFn })(null, {
        lg: [{ w: 6, minW: 4, h: 6, minH: 4 }],
      })

      expect(mockFn).toHaveBeenCalledWith({
        value: {
          lg: [{ w: 6, minW: 4, h: 6, minH: 4 }],
        },
      })
    })
  })
})
