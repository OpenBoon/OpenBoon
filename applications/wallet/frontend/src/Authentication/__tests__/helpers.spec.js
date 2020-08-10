import user from '../../User/__mocks__/user'

import { authenticateUser, logout } from '../helpers'

const noop = () => () => {}

describe('<Authentication /> helpers', () => {
  describe('authenticateUser()', () => {
    it('should authenticate the user with a username/password', async () => {
      fetch.mockResponseOnce(JSON.stringify({ id: 12345 }))

      const mockSetErrorMessage = jest.fn()
      const mockMutate = jest.fn()

      require('swr').__setMockMutateFn(mockMutate)

      await authenticateUser({
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

      expect(mockMutate).toHaveBeenCalledWith({ id: 12345, projectId: '' })
    })

    it('should authenticate the user with a Google JWT', async () => {
      fetch.mockResponseOnce(JSON.stringify(user))

      const mockSetErrorMessage = jest.fn()
      const mockMutate = jest.fn()

      require('swr').__setMockMutateFn(mockMutate)

      await authenticateUser({
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

      expect(mockMutate).toHaveBeenCalledWith(user)
    })

    it('should authenticate a user with no project', async () => {
      fetch.mockResponseOnce(JSON.stringify({ ...user, roles: {} }))

      const mockMutate = jest.fn()

      require('swr').__setMockMutateFn(mockMutate)

      await authenticateUser({
        setErrorMessage: noop,
      })({
        idToken: 'ID_TOKEN',
      })

      expect(mockMutate).toHaveBeenCalledWith({
        ...user,
        roles: {},
        projectId: '',
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

    it('should display an alert for locked out user', async () => {
      const mockSetErrorMessage = jest.fn()

      fetch.mockResponseOnce('Locked Out', { status: 423 })

      await authenticateUser({
        setErrorMessage: mockSetErrorMessage,
      })({
        username: 'username',
        password: 'password',
      })

      expect(mockSetErrorMessage).toHaveBeenCalledWith(
        'Your account has been locked due to too many failed login attempts.',
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
      const mockMutate = jest.fn()
      const mockRouterPush = jest.fn()

      require('swr').__setMockMutateFn(mockMutate)
      require('next/router').__setMockPushFunction(mockRouterPush)

      await logout({
        googleAuth: { signOut: mockSignOut },
      })({ redirectUrl: '/', redirectAs: '/' })

      expect(mockSignOut).toHaveBeenCalled()

      expect(mockMutate).toHaveBeenCalledWith({})

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/logout/')
      expect(fetch.mock.calls[0][1]).toEqual({
        headers: {
          'X-CSRFToken': 'CSRF_TOKEN',
          'Content-Type': 'application/json;charset=UTF-8',
        },
        method: 'POST',
      })

      expect(mockRouterPush).toHaveBeenCalledWith('/', '/')
    })
  })
})
