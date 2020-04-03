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

      const data = await meFetcher('/url')

      expect(data).toEqual({ id: 12345 })
    })

    it('should return an empty object in case of error', async () => {
      fetch.mockResponseOnce(null, { status: 500 })

      try {
        await meFetcher('/url')
      } catch (response) {
        expect(response.status).toBe(500)

        expect(response.statusText).toBe('Internal Server Error')

        expect(response).toEqual({})
      }
    })
  })
})
