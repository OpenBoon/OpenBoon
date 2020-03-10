import { authenticateUser, logout } from '../helpers'

describe('<Authentication /> helpers', () => {
  describe('authenticateUser()', () => {
    it('should authenticate the user with a username/password', async () => {
      fetch.mockResponseOnce(JSON.stringify({ id: 12345 }))

      const mockSetErrorMessage = jest.fn()
      const mockSetUser = jest.fn()

      await authenticateUser({
        setUser: mockSetUser,
        setErrorMessage: mockSetErrorMessage,
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

      expect(mockSetUser).toHaveBeenCalledWith({
        user: { id: 12345, projectId: '' },
      })
    })

    it('should authenticate the user with a Google JWT', async () => {
      fetch.mockResponseOnce(JSON.stringify({ id: 12345 }))

      const mockSetErrorMessage = jest.fn()
      const mockSetUser = jest.fn()

      await authenticateUser({
        setUser: mockSetUser,
        setErrorMessage: mockSetErrorMessage,
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

      expect(mockSetUser).toHaveBeenCalledWith({
        user: { id: 12345, projectId: '' },
      })
    })

    it('should display an alert for incorrect username/password', async () => {
      const mockSetErrorMessage = jest.fn()

      fetch.mockResponseOnce('Access Denied', { status: 401 })

      await authenticateUser({
        setErrorMessage: mockSetErrorMessage,
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
      const mockRouterPush = jest.fn()

      require('next/router').__setMockPushFunction(mockRouterPush)

      await logout({
        googleAuth: { signOut: mockSignOut },
        setUser: mockSetUser,
      })({ redirectUrl: '/' })

      expect(mockSignOut).toHaveBeenCalled()

      expect(mockSetUser).toHaveBeenCalledWith({ user: null })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/logout/')
      expect(fetch.mock.calls[0][1]).toEqual({
        headers: {
          'X-CSRFToken': 'CSRF_TOKEN',
          'Content-Type': 'application/json;charset=UTF-8',
        },
        method: 'POST',
      })

      expect(mockRouterPush).toHaveBeenCalledWith('/')
    })
  })
})
