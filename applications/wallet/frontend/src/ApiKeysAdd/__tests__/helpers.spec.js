import apikeysadd from '../__mocks__/apikeysadd'

import { onSubmit } from '../helpers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ApiKeysAdd /> helpers', () => {
  describe('onSubmit()', () => {
    it('should call the API', () => {
      fetch.mockResponseOnce(JSON.stringify(apikeysadd))

      onSubmit({ projectId: PROJECT_ID })({
        name: 'FooBarApiKey',
        permissions: {
          SuperAdmin: true,
          ProjectAdmin: false,
          AssetsRead: true,
          AssetsImport: false,
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
})
