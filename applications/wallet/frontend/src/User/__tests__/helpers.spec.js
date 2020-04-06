import user from '../__mocks__/user'

import { noop, meFetcher } from '../helpers'

describe('<User /> helpers', () => {
  describe('noop()', () => {
    it('should do nothing', () => {
      expect(noop()).toBe(undefined)
    })
  })

  describe('meFetcher()', () => {
    it('should fetch data', async () => {
      fetch.mockResponseOnce(JSON.stringify({ id: 12345 }))

      const data = await meFetcher('/me')

      expect(data).toEqual({ id: 12345, projectId: '' })
    })

    it('should use the first project as default', async () => {
      fetch.mockResponseOnce(JSON.stringify(user))

      const data = await meFetcher('/me')

      expect(data).toEqual(user)
    })

    it('should set an empty projectId', async () => {
      fetch.mockResponseOnce(JSON.stringify({ ...user, roles: {} }))

      const data = await meFetcher('/me')

      expect(data).toEqual({ ...user, roles: {}, projectId: '' })
    })

    it('should return an empty object in case of error', async () => {
      fetch.mockResponseOnce(null, { status: 500 })

      try {
        await meFetcher('/me')
      } catch (response) {
        expect(response.status).toBe(500)

        expect(response.statusText).toBe('Internal Server Error')

        expect(response).toEqual({})
      }
    })
  })
})
