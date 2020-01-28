import { onSubmit } from '../helpers'

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
        errors: {},
      })
    })

    it('should display an error message with mismatching new passwords', async () => {
      const mockDispatch = jest.fn()

      fetch.mockRejectOnce({
        json: () => Promise.resolve({ newPassword2: ['Error message'] }),
      })

      await onSubmit({
        dispatch: mockDispatch,
        projectId: 'projectId',
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
        errors: {
          newPassword2: 'Error message',
        },
      })
    })
  })
})
