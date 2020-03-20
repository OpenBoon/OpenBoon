import { onCopy } from '../helpers'

describe('<Copy /> helpers', () => {
  describe('onCopy()', () => {
    it('should copy text to clipboard', () => {
      const mockRef = { current: { select: jest.fn(), blur: jest.fn() } }

      const mockFn = jest.fn()

      Object.defineProperty(document, 'execCommand', { value: mockFn })

      onCopy({ copyRef: mockRef })

      expect(mockRef.current.select).toHaveBeenCalled()
      expect(mockFn).toHaveBeenCalledWith('copy')
      expect(mockRef.current.blur).toHaveBeenCalled()
    })
  })
})
