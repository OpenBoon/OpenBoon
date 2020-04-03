import { initializeFetcher, fetcher } from '../helpers'

const noop = () => () => {}

describe('<Fetch /> helpers', () => {
  describe('fetcher()', () => {
    it('should fetch data', async () => {
      initializeFetcher({ mutate: noop })

      fetch.mockResponseOnce(JSON.stringify({ id: 12345 }))

      const data = await fetcher('/url')

      expect(data).toEqual({ id: 12345 })
    })

    it('should return the raw response in case of error', async () => {
      initializeFetcher({ mutate: noop })

      fetch.mockResponseOnce(null, { status: 500 })

      try {
        await fetcher('/url')
      } catch (response) {
        expect(response.status).toBe(500)

        expect(response.statusText).toBe('Internal Server Error')

        expect(response).toMatchSnapshot()
      }
    })

    it('should return the raw response if its not a json', async () => {
      initializeFetcher({ mutate: noop })

      fetch.mockResponseOnce(null, { status: 200 })

      try {
        await fetcher('/url')
      } catch (response) {
        expect(response.status).toBe(200)

        expect(response.statusText).toBe('Ok')

        expect(response).toMatchSnapshot()
      }
    })

    it('should logout the user', async () => {
      const mockMutate = jest.fn()

      initializeFetcher({ mutate: mockMutate })

      fetch.mockResponseOnce(null, { status: 401 })

      const data = await fetcher('/url')

      expect(data).toEqual({})

      expect(mockMutate).toHaveBeenCalledWith({}, false)
    })
  })
})
