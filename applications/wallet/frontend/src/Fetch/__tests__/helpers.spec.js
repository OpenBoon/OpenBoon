import { USER } from '../../User/helpers'

import { initializeFetcher, fetcher } from '../helpers'

const noop = () => () => {}

describe('<Fetch /> helpers', () => {
  describe('fetcher()', () => {
    it('should fetch data', async () => {
      initializeFetcher({ setUser: noop })

      fetch.mockResponseOnce(JSON.stringify({ id: 12345 }))

      const data = await fetcher('/url')

      expect(data).toEqual({ id: 12345 })
    })

    it('should return the raw response in case of error', async () => {
      initializeFetcher({ setUser: noop })

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
      initializeFetcher({ setUser: noop })

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
      const mockSetUser = jest.fn()
      const mockRemoveItem = jest.fn()

      initializeFetcher({ setUser: mockSetUser })

      fetch.mockResponseOnce(null, { status: 401 })

      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          removeItem: mockRemoveItem,
        },
      })

      const data = await fetcher('/url')

      expect(data).toEqual({})

      expect(mockSetUser).toHaveBeenCalledWith({})

      expect(mockRemoveItem).toHaveBeenCalledWith(USER)
    })
  })
})
