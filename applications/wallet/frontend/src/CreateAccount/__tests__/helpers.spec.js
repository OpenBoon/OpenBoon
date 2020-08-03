import { onConfirm } from '../helpers'

describe('<CreateAccount /> helpers', () => {
  describe('onConfirm()', () => {
    it('should redirect properly', async () => {
      const mockFn = jest.fn()

      require('next/router').__setMockPushFunction(mockFn)

      fetch.mockResponseOnce(null, { status: 500 })

      await onConfirm({ uid: 2, token: 'f1c5b71f-bc9d-4b54-aa69-cbec03f94f5e' })

      expect(fetch.mock.calls[0][0]).toEqual('/api/v1/accounts/confirm')

      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: JSON.stringify({
          userId: 2,
          token: 'f1c5b71f-bc9d-4b54-aa69-cbec03f94f5e',
        }),
      })

      expect(mockFn).toHaveBeenCalledWith(
        '/create-account?action=account-activation-expired',
        '/create-account',
      )
    })
  })
})
