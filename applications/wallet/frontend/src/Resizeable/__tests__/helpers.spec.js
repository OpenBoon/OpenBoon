import { getHandleMouseMove, getHandleMouseUp } from '../helpers'

const noop = () => {}

describe('<Panel /> helpers', () => {
  describe('getHandleMouseMove()', () => {
    it('should handle move while open', () => {
      const mockFn = jest.fn()

      getHandleMouseMove({
        isOpen: true,
        size: 400,
        dispatch: mockFn,
      })({
        difference: 20,
      })

      expect(mockFn).toHaveBeenCalledWith({
        type: 'HANDLE_MOUSE_MOVE',
        payload: { newSize: 380 },
      })
    })

    it('should handle move while closed', () => {
      const mockFn = jest.fn()

      getHandleMouseMove({
        isOpen: false,
        size: 400,
        dispatch: mockFn,
      })({
        difference: -20,
      })

      expect(mockFn).toHaveBeenCalledWith({
        type: 'HANDLE_MOUSE_MOVE',
        payload: { newSize: 20 },
      })
    })
  })

  describe('getHandleMouseUp()', () => {
    it('should handle move while open', () => {
      const mockFn = jest.fn()

      getHandleMouseUp({
        isOpen: true,
        size: 400,
        minSize: 400,
        onMouseUp: ({ newSize }) => ({ extra: newSize }),
        dispatch: mockFn,
      })({
        difference: 20,
      })

      expect(mockFn).toHaveBeenCalledWith({
        type: 'HANDLE_MOUSE_UP',
        payload: { newSize: 380, minSize: 400, extra: 380 },
      })
    })

    it('should handle move while closed', () => {
      const mockFn = jest.fn()

      getHandleMouseUp({
        isOpen: false,
        size: 400,
        minSize: 400,
        onMouseUp: noop,
        dispatch: mockFn,
      })({
        difference: -20,
      })
      expect(mockFn).toHaveBeenCalledWith({
        type: 'HANDLE_MOUSE_UP',
        payload: { newSize: 20, minSize: 400 },
      })
    })
  })
})
