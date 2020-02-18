import projectUsersAdd from '../__mocks__/projectUsersAdd'

import { onSubmit, onCopy } from '../helpers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ProjectUsersAdd /> helpers', () => {
  describe('onSubmit()', () => {
    it('should call the API', async () => {
      const mockFn = jest.fn()

      fetch.mockResponseOnce(JSON.stringify(projectUsersAdd))

      await onSubmit({
        dispatch: mockFn,
        projectId: PROJECT_ID,
        state: {
          emails: 'jane@zorroa.com, joe@zorroa.com',
          permissions: {
            SuperAdmin: true,
            ProjectAdmin: false,
            AssetsRead: true,
            AssetsImport: false,
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
              permissions: ['SuperAdmin', 'AssetsRead'],
            },
            {
              email: 'joe@zorroa.com',
              permissions: ['SuperAdmin', 'AssetsRead'],
            },
          ],
        }),
      })

      expect(mockFn).toHaveBeenCalledWith(projectUsersAdd.results)
    })

    it('should error', async () => {
      const mockFn = jest.fn()

      fetch.mockRejectOnce({
        json: () => Promise.resolve({ name: ["This email doesn't work."] }),
      })

      await onSubmit({
        dispatch: mockFn,
        projectId: PROJECT_ID,
        state: {
          emails: 'jane@zorroa.com, joe@zorroa.com',
          permissions: {
            SuperAdmin: true,
            ProjectAdmin: true,
            AssetsRead: true,
            AssetsImport: false,
          },
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)

      expect(fetch.mock.calls[0][0]).toEqual(
        '/api/v1/projects/76917058-b147-4556-987a-0a0f11e46d9b/users/',
      )

      expect(mockFn).toHaveBeenCalledWith({
        errors: {
          name: "This email doesn't work.",
        },
      })
    })
  })

  describe('onCopy()', () => {
    it('should copy text to clipboard', () => {
      const mockRef = { current: { select: jest.fn(), blur: jest.fn() } }

      const mockFn = jest.fn()

      Object.defineProperty(document, 'execCommand', { value: mockFn })

      onCopy({ inputRef: mockRef })

      expect(mockRef.current.select).toHaveBeenCalledWith()
      expect(mockFn).toHaveBeenCalledWith('copy')
      expect(mockRef.current.blur).toHaveBeenCalledWith()
    })
  })
})
