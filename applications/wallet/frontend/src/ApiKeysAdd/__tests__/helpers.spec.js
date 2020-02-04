import apiKey from '../../ApiKey/__mocks__/apiKey'

import { onSubmit, onCopy } from '../helpers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ApiKeysAdd /> helpers', () => {
  describe('onSubmit()', () => {
    it('should call the API', () => {
      const mockFn = jest.fn()

      fetch.mockResponseOnce(JSON.stringify(apiKey))

      onSubmit({
        dispatch: mockFn,
        projectId: PROJECT_ID,
        state: {
          name: 'FooBarApiKey',
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
        '/api/v1/projects/76917058-b147-4556-987a-0a0f11e46d9b/apikeys/',
      )

      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: JSON.stringify({
          name: 'FooBarApiKey',
          permissions: ['SuperAdmin', 'AssetsRead'],
        }),
      })
    })
  })

  it('should display an error message with mismatching new passwords', async () => {
    const mockFn = jest.fn()

    fetch.mockRejectOnce({
      json: () =>
        Promise.resolve({ name: ['This API key name is already in use.'] }),
    })

    await onSubmit({
      dispatch: mockFn,
      projectId: PROJECT_ID,
      state: {
        name: 'FooBarApiKey',
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
      '/api/v1/projects/76917058-b147-4556-987a-0a0f11e46d9b/apikeys/',
    )

    expect(mockFn).toHaveBeenCalledWith({
      errors: {
        name: 'This API key name is already in use.',
      },
    })
  })

  describe('onCopy()', () => {
    it('should copy text to clipboard', () => {
      const mockRef = { current: { select: jest.fn(), blur: jest.fn() } }

      const mockFn = jest.fn()

      Object.defineProperty(document, 'execCommand', { value: mockFn })

      onCopy({ textareaRef: mockRef })

      expect(mockRef.current.select).toHaveBeenCalledWith()
      expect(mockFn).toHaveBeenCalledWith('copy')
      expect(mockRef.current.blur).toHaveBeenCalledWith()
    })
  })
})
