import { getUser, initializeUserstorer } from '../../Authentication/helpers'

import { onSubmit } from '../helpers'

const USER_ID = 'fe39c66b-68f8-4d59-adfd-395f6baaf72c'

describe('<AccountProfile /> helpers', () => {
  describe('onSubmit()', () => {
    it('should update the first and last name ', async () => {
      const mockDispatch = jest.fn()

      initializeUserstorer({ setUser: jest.fn() })

      fetch.mockResponseOnce(
        JSON.stringify({
          firstName: 'John',
          lastName: 'Smith',
        }),
      )

      await onSubmit({
        dispatch: mockDispatch,
        state: {
          id: USER_ID,
          firstName: 'John',
          lastName: 'Smith',
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual(`/api/v1/users/${USER_ID}/`)
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
        showForm: false,
        success: true,
        errors: {},
      })

      expect(getUser()).toEqual({
        firstName: 'John',
        lastName: 'Smith',
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
          id: USER_ID,
          firstName: 'John',
          lastName: 'Smith',
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual(`/api/v1/users/${USER_ID}/`)
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: '{"firstName":"John","lastName":"Smith"}',
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        success: false,
        errors: { firstName: 'Error message' },
      })
    })
  })
})
