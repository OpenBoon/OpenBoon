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
    it('should authenticate the user with a username/password', async () => {
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

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/login/')
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: { 'Content-Type': 'application/json;charset=UTF-8' },
        body: '{"username":"username","password":"password"}',
      })

      expect(mockSetErrorMessage).toHaveBeenCalledWith('')

      expect(mockSetUser).toHaveBeenCalledWith({ id: 12345 })

      expect(mockSetItem).toHaveBeenCalledWith(
        USER,
        JSON.stringify({ id: 12345 }),
      )
    })

    it('should authenticate the user with a Google JWT', async () => {
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
        idToken: 'ID_TOKEN',
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/login/')
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          Authorization: 'Bearer ID_TOKEN',
          'Content-Type': 'application/json;charset=UTF-8',
        },
      })

      expect(mockSetErrorMessage).toHaveBeenCalledWith('')

      expect(mockSetUser).toHaveBeenCalledWith({ id: 12345 })

      expect(mockSetItem).toHaveBeenCalledWith(
        USER,
        JSON.stringify({ id: 12345 }),
      )
    })

    it('should display an alert for incorrect username/password', async () => {
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

    it('should display an alert for any other error', async () => {
      const mockSetErrorMessage = jest.fn()

      fetch.mockResponseOnce('Access Denied', { status: 500 })

      await authenticateUser({
        setErrorMessage: mockSetErrorMessage,
        setUser: noop,
      })({
        username: 'username',
        password: 'password',
      })

      expect(mockSetErrorMessage).toHaveBeenCalledWith('Network error.')
    })
  })

  describe('logout()', () => {
    it('should logout the user', async () => {
      const mockSignOut = jest.fn()
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

      await logout({
        googleAuth: { signOut: mockSignOut },
        setUser: mockSetUser,
      })()

      expect(mockSignOut).toHaveBeenCalledWith()

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
