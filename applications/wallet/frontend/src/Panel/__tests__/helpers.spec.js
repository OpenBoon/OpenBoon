import { handleMouseUp } from '../helpers'

describe('<Panel /> helpers', () => {
  describe('handleMouseUp()', () => {
    it('should close panel when smaller than minWidth', () => {
      const mockFn = jest.fn()

      const onMouseUp = handleMouseUp({ minWidth: 300, setOpenPanel: mockFn })

      onMouseUp({ size: 200 })

      expect(mockFn).toHaveBeenCalledWith({ value: '' })
    })

    it('do nothing when larger than  minWidth', () => {
      const mockFn = jest.fn()

      const onMouseUp = handleMouseUp({ minWidth: 300, setOpenPanel: mockFn })

      onMouseUp({ size: 400 })

      expect(mockFn).not.toHaveBeenCalled()
    })
  })
})
