import { onBlur } from '../helpers'

describe('<UserMenu /> helpers', () => {
  describe('onBlur()', () => {
    it('onBlur should call setMenuOpen when a blur happens outside of the container', () => {
      const setMenuOpen = jest.fn()

      const container = {
        current: {
          contains: jest.fn(
            relatedTarget => relatedTarget === 'includes' || false,
          ),
        },
      }

      onBlur({ container, setMenuOpen })({ relatedTarget: 'excludes' })

      expect(setMenuOpen).toHaveBeenCalledWith(false)
    })

    it('onBlur should not call setMenuOpen when a blur happens inside of the container', () => {
      const setMenuOpen = jest.fn()

      const container = {
        current: {
          contains: jest.fn(
            relatedTarget => relatedTarget === 'includes' || false,
          ),
        },
      }

      onBlur({ container, setMenuOpen })({ relatedTarget: 'includes' })

      expect(setMenuOpen).not.toHaveBeenCalled()
    })
  })
})
