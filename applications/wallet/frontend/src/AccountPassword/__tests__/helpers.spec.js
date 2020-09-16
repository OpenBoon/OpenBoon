import { onSubmit, onReset } from '../helpers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

const noop = () => () => {}

describe('<AccountPassword /> helpers', () => {
  describe('onSubmit()', () => {
    it('should update the password ', async () => {
      const mockDispatch = jest.fn()

      fetch.mockResponseOnce(
        JSON.stringify({
          oldPassword: 'password',
          newPassword1: 'password1',
          newPassword2: 'password1',
        }),
      )

      await onSubmit({
        dispatch: mockDispatch,
        projectId: 'iud',
        state: {
          currentPassword: 'password',
          newPassword: 'password1',
          confirmPassword: 'password1',
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/password/change/')
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body:
          '{"oldPassword":"password","newPassword1":"password1","newPassword2":"password1"}',
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
        success: true,
        isLoading: false,
        errors: {},
      })
    })

    it('should handle errors', async () => {
      const mockDispatch = jest.fn()

      fetch.mockRejectOnce({
        json: () => Promise.resolve({ newPassword2: ['Error message'] }),
      })

      await onSubmit({
        dispatch: mockDispatch,
        projectId: PROJECT_ID,
        state: {
          currentPassword: 'password',
          newPassword: 'password1',
          confirmPassword: 'password2',
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/password/change/')
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body:
          '{"oldPassword":"password","newPassword1":"password1","newPassword2":"password2"}',
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        success: false,
        isLoading: false,
        errors: { newPassword2: 'Error message' },
      })
    })
  })

  describe('onReset()', () => {
    it('should send a reset password request', async () => {
      const mockSetError = jest.fn()
      const mockMutate = jest.fn()
      const mockSignOut = jest.fn()
      const mockRouterPush = jest.fn()

      require('swr').__setMockMutateFn(mockMutate)
      require('next/router').__setMockPushFunction(mockRouterPush)

      fetch.mockResponseOnce(JSON.stringify({ email: 'jane@zorroa.com' }))

      await onReset({
        setError: mockSetError,
        email: 'jane@zorroa.com',
        googleAuth: {
          signOut: mockSignOut,
        },
      })

      expect(fetch.mock.calls.length).toEqual(2)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/password/reset/')
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: '{"email":"jane@zorroa.com"}',
      })

      expect(mockSetError).not.toHaveBeenCalled()
      expect(mockMutate).toHaveBeenCalledWith({})
      expect(mockSignOut).toHaveBeenCalled()
      expect(mockRouterPush).toHaveBeenCalledWith(
        '/?action=password-reset-request-success',
        '/',
      )
    })

    it('should dispatch an error', async () => {
      const mockSetError = jest.fn()

      fetch.mockResponseOnce(null, { status: 400 })

      await onReset({
        setError: mockSetError,
        email: 'jane@zorroa.com',
        googleAuth: noop,
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/password/reset/')
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: '{"email":"jane@zorroa.com"}',
      })

      expect(mockSetError).toHaveBeenCalledWith(
        'Something went wrong. Please try again.',
      )
    })
  })
})
