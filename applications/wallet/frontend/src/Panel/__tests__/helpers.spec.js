import { onMouseUp } from '../helpers'

describe('<Panel /> helpers', () => {
  describe('onMouseUp()', () => {
    it('should handle resizing above minWidth', () => {
      expect(
        onMouseUp({
          minWidth: 400,
        })({
          newSize: 400,
        }),
      ).toEqual({})
    })

    it('should handle resizing below minWidth', () => {
      expect(
        onMouseUp({
          minWidth: 400,
        })({
          newSize: 100,
        }),
      ).toEqual({ openPanel: '' })
    })
  })
})
