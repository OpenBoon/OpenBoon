import mockUser from '../../User/__mocks__/user'

import { onSubmit } from '../helpers'

const { id } = mockUser

describe('<AccountProfile /> helpers', () => {
  describe('onSubmit()', () => {
    it('should update the first and last name ', async () => {
      const mockDispatch = jest.fn()

      fetch.mockResponseOnce(
        JSON.stringify({
          firstName: 'John',
          lastName: 'Smith',
        }),
      )

      await onSubmit({
        dispatch: mockDispatch,
        state: {
          id,
          firstName: 'John',
          lastName: 'Smith',
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual(`/api/v1/users/${id}/`)
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: '{"firstName":"John","lastName":"Smith"}',
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        firstName: 'John',
        lastName: 'Smith',
        errors: {},
      })
    })

    it('should display an error message', async () => {
      const mockDispatch = jest.fn()

      fetch.mockRejectOnce({
        json: () => Promise.resolve({ firstName: ['Error message'] }),
      })

      await onSubmit({
        dispatch: mockDispatch,
        state: {
          id,
          firstName: 'John',
          lastName: 'Smith',
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual(`/api/v1/users/${id}/`)
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: '{"firstName":"John","lastName":"Smith"}',
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        errors: { firstName: 'Error message' },
      })
    })
  })
})
