import organizationOwnersAdd from '../__mocks__/organizationOwnersAdd'

import { onSubmit } from '../helpers'

const ORGANIZATION_ID = '42869703-fb62-4988-a0d1-e59b15caff06'

describe('<OrganizationOwnersAdd /> helpers', () => {
  describe('onSubmit()', () => {
    it('should call the API', async () => {
      const mockFn = jest.fn()

      fetch.mockResponseOnce(JSON.stringify(organizationOwnersAdd), {
        headers: { 'content-type': 'application/json' },
      })

      await onSubmit({
        dispatch: mockFn,
        organizationId: ORGANIZATION_ID,
        state: {
          emails: 'jane@zorroa.com, joe@zorroa.com',
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)

      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/organizations/${ORGANIZATION_ID}/owners/`,
      )

      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: JSON.stringify({
          emails: ['jane@zorroa.com', 'joe@zorroa.com'],
        }),
      })

      expect(mockFn).toHaveBeenCalledWith({
        ...organizationOwnersAdd.results,
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
        organizationId: ORGANIZATION_ID,
        state: {
          emails: 'jane@zorroa.com, joe@zorroa.com',
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)

      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/organizations/${ORGANIZATION_ID}/owners/`,
      )

      expect(mockFn).toHaveBeenCalledWith({
        isLoading: false,
        errors: { emails: "This email doesn't work." },
      })
    })
  })
})
