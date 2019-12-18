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

      const mockSetErrorMessage = jest.fn()
      const mockSetUser = jest.fn()
      const mockSetItem = jest.fn()

      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          setItem: mockSetItem,
        },
      })

      await authenticateUser({
        setErrorMessage: mockSetErrorMessage,
        setUser: mockSetUser,
      })({
        username: 'username',
        password: 'password',
      })

      expect(mockSetErrorMessage).toHaveBeenCalledWith('')

      expect(mockSetUser).toHaveBeenCalledWith({ id: 12345 })

      expect(mockSetItem).toHaveBeenCalledWith(
        USER,
        JSON.stringify({ id: 12345 }),
      )
    })

    it('should display an alert for incorrect email/password', async () => {
      const mockSetErrorMessage = jest.fn()

      fetch.mockResponseOnce('Access Denied', { status: 401 })

      await authenticateUser({
        setErrorMessage: mockSetErrorMessage,
        setUser: noop,
      })({
        username: 'username',
        password: 'password',
      })

      expect(mockSetErrorMessage).toHaveBeenCalledWith(
        'Invalid email or password.',
      )
    })
  })

  describe('logout()', () => {
    it('should logout the user', async () => {
      const mockSetUser = jest.fn()
      const mockRemoveItem = jest.fn()

      Object.defineProperty(document, 'cookie', {
        writable: true,
        value: 'csrftoken=CSRF_TOKEN',
      })

      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          removeItem: mockRemoveItem,
        },
      })

      await logout({ setUser: mockSetUser })()

      expect(mockSetUser).toHaveBeenCalledWith({})

      expect(mockRemoveItem).toHaveBeenCalledWith(USER)

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/logout/')
      expect(fetch.mock.calls[0][1]).toEqual({
        headers: {
          'X-CSRFToken': 'CSRF_TOKEN',
          'Content-Type': 'application/json;charset=UTF-8',
        },
        method: 'POST',
      })
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
