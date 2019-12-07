import { USER, getUser, authenticateUser, logout, fetcher } from '../helpers'

const noop = () => () => {}

describe('<Authentication /> helpers', () => {
  describe('getUser()', () => {
    it('should return no user', () => {
      expect(getUser()).toEqual({})
    })

    it('should return no user', () => {
      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          getItem: () => 'not a json object',
        },
      })
      expect(getUser()).toEqual({})
    })

    it('should return a user', () => {
      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          getItem: () => JSON.stringify({ id: 12345 }),
        },
      })

      expect(getUser()).toEqual({ id: 12345 })
    })
  })

  describe('authenticateUser()', () => {
    it('should authenticate the user', async () => {
      fetch.mockResponseOnce(JSON.stringify({ id: 12345 }))

      const mockSetUser = jest.fn()
      const mockSetItem = jest.fn()

      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          setItem: mockSetItem,
        },
      })

      await authenticateUser({ setUser: mockSetUser })({
        username: 'username',
        password: 'password',
      })

      expect(mockSetUser).toHaveBeenCalledWith({ id: 12345 })

      expect(mockSetItem).toHaveBeenCalledWith(
        USER,
        JSON.stringify({ id: 12345 }),
      )
    })
  })

  describe('logout()', () => {
    it('should logout the user', async () => {
      const mockSetUser = jest.fn()
      const mockRemoveItem = jest.fn()

      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          removeItem: mockRemoveItem,
        },
      })

      await logout({ setUser: mockSetUser })()

      expect(mockSetUser).toHaveBeenCalledWith({})

      expect(mockRemoveItem).toHaveBeenCalledWith(USER)
    })
  })

  describe('fetcher()', () => {
    it('should fetch data', async () => {
      fetch.mockResponseOnce(JSON.stringify({ id: 12345 }))

      const data = await fetcher({ setUser: noop })()

      expect(data).toEqual({ id: 12345 })
    })

    it('should logout the user', async () => {
      const mockSetUser = jest.fn()
      const mockRemoveItem = jest.fn()

      fetch.mockResponseOnce('Access Denied', { status: 401 })

      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          removeItem: mockRemoveItem,
        },
      })

      const data = await fetcher({ setUser: mockSetUser })()

      expect(data).toEqual({})

      expect(mockSetUser).toHaveBeenCalledWith({})

      expect(mockRemoveItem).toHaveBeenCalledWith(USER)
    })
  })
})
