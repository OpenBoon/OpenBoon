import { onSubmit } from '../helpers'

const USER_ID = 42

describe('<AccountProfile /> helpers', () => {
  describe('onSubmit()', () => {
    it('should update the first and last name ', async () => {
      let user = {}

      const mockMutate = jest.fn((cb) => {
        user = cb(user)
      })
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
          id: USER_ID,
          firstName: 'John',
          lastName: 'Smith',
        },
        mutate: mockMutate,
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
        showForm: false,
        success: true,
        errors: {},
      })

      expect(user).toEqual({ firstName: 'John', lastName: 'Smith' })
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
