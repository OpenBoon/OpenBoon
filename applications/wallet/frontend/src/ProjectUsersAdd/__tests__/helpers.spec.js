import projectUsersAdd from '../__mocks__/projectUsersAdd'

import { onSubmit } from '../helpers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ProjectUsersAdd /> helpers', () => {
  describe('onSubmit()', () => {
    it('should call the API', async () => {
      const mockFn = jest.fn()

      fetch.mockResponseOnce(JSON.stringify(projectUsersAdd), {
        headers: { 'content-type': 'application/json' },
      })

      await onSubmit({
        dispatch: mockFn,
        projectId: PROJECT_ID,
        state: {
          emails: 'jane@zorroa.com, joe@zorroa.com',
          roles: {
            ML_Tools: true,
            API_Keys: false,
            User_Admin: true,
          },
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)

      expect(fetch.mock.calls[0][0]).toEqual(
        '/api/v1/projects/76917058-b147-4556-987a-0a0f11e46d9b/users/',
      )

      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: JSON.stringify({
          batch: [
            {
              email: 'jane@zorroa.com',
              roles: ['ML_Tools', 'User_Admin'],
            },
            {
              email: 'joe@zorroa.com',
              roles: ['ML_Tools', 'User_Admin'],
            },
          ],
        }),
      })

      expect(mockFn).toHaveBeenCalledWith({
        ...projectUsersAdd.results,
        isLoading: false,
      })
    })

    it('should handle errors', async () => {
      const mockFn = jest.fn()

      fetch.mockRejectOnce({
        json: () => Promise.resolve({ emails: ["This email doesn't work."] }),
      })

      await onSubmit({
        dispatch: mockFn,
        projectId: PROJECT_ID,
        state: {
          emails: 'jane@zorroa.com, joe@zorroa.com',
          roles: {
            ML_Tools: true,
            API_Keys: false,
            User_Admin: true,
          },
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)

      expect(fetch.mock.calls[0][0]).toEqual(
        '/api/v1/projects/76917058-b147-4556-987a-0a0f11e46d9b/users/',
      )

      expect(mockFn).toHaveBeenCalledWith({
        isLoading: false,
        errors: { emails: "This email doesn't work." },
      })
    })
  })
})
