import { onRequest } from '../helpers'

describe('<ResetPassword /> helpers', () => {
  describe('onRequest()', () => {
    it('should display an error message', async () => {
      const mockDispatch = jest.fn()

      fetch.mockResponseOnce(null, { status: 400 })

      await onRequest({
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
        isLoading: false,
        error: 'Error. Please try again.',
      })
    })
  })
})
