import {
  getTokens,
  isUserAuthenticated,
  getTokenTimeout,
  authenticateUser,
  logout,
} from '../helpers'

jest.mock('../../Axios/helpers')

describe('<Authentication /> helpers', () => {
  describe('getTokens()', () => {
    it('should return no tokens', () => {
      expect(getTokens()).toEqual({})
    })

    it('should return tokens', () => {
      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          getItem: key => key,
        },
      })

      expect(getTokens()).toEqual({
        accessToken: 'ACCESS_TOKEN',
        refreshToken: 'REFRESH_TOKEN',
      })
    })
  })

  describe('isUserAuthenticated()', () => {
    it('should return true if the refreshToken is in the future', () => {
      expect(isUserAuthenticated({ now: 1000, refreshToken: 9000 })).toBe(true)
    })

    it('should return false if the refreshToken is in the past', () => {
      expect(isUserAuthenticated({ now: 9000, refreshToken: 1000 })).toBe(false)
    })

    it('should return false if the refreshToken is undefined', () => {
      expect(isUserAuthenticated({})).toBe(false)
    })
  })

  describe('getTokenTimeout()', () => {
    it('should get the token timeout', () => {
      expect(
        getTokenTimeout({
          now: 10000,
          refreshToken: 90000,
        }),
      ).toEqual(50000)
    })
  })

  describe('authenticateUser()', () => {
    it('should authenticate the user', () => {
      const mockSetUser = jest.fn()
      const mockSetItem = jest.fn()

      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          setItem: mockSetItem,
        },
      })

      authenticateUser({ setUser: mockSetUser })({
        username: 'username',
        password: 'password',
      })

      expect(mockSetUser).toHaveBeenCalledWith({ isAuthenticated: true })

      expect(mockSetItem).toHaveBeenCalledWith('ACCESS_TOKEN', 'access')
      expect(mockSetItem).toHaveBeenCalledWith('REFRESH_TOKEN', 'refresh')
    })
  })

  describe('logout()', () => {
    it('should logout the user', () => {
      const mockSetUser = jest.fn()
      const mockRemoveItem = jest.fn()

      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          removeItem: mockRemoveItem,
        },
      })

      logout({ setUser: mockSetUser })()

      expect(mockSetUser).toHaveBeenCalledWith({ isAuthenticated: false })

      expect(mockRemoveItem).toHaveBeenCalledWith('ACCESS_TOKEN')
      expect(mockRemoveItem).toHaveBeenCalledWith('REFRESH_TOKEN')
    })
  })
})
