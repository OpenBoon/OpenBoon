import { onSubmit } from '../helpers'

describe('<ResetPassword /> helpers', () => {
  describe('onSubmit()', () => {
    it('should send a reset password request ', async () => {
      const mockFn = jest.fn()
      const mockDispatch = jest.fn()

      require('next/router').__setMockPushFunction(mockFn)

      fetch.mockResponseOnce(
        JSON.stringify({
          email: 'username',
        }),
      )

      await onSubmit({
        dispatch: mockDispatch,
        state: { email: 'username' },
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/password/reset/')
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: '{"email":"username"}',
      })
      expect(mockFn).toBeCalledWith(
        '/reset-password/?action=password-reset-request-success',
      )
    })

    it('should display an error message', async () => {
      const mockDispatch = jest.fn()

      fetch.mockRejectOnce(null, { status: 500 })

      await onSubmit({
        dispatch: mockDispatch,
        state: { email: 'username' },
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/password/reset/')
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: '{"email":"username"}',
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        error: 'Something went wrong. Please try again.',
      })
    })
  })
})
