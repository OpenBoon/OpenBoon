import { USER } from '../../Authentication/helpers'

import { initialize, fetcher } from '../helpers'

const noop = () => () => {}

describe('<Fetch /> helpers', () => {
  describe('fetcher()', () => {
    it('should fetch data', async () => {
      initialize({ setUser: noop })

      fetch.mockResponseOnce(JSON.stringify({ id: 12345 }))

      const data = await fetcher()

      expect(data).toEqual({ id: 12345 })
    })

    it('should logout the user', async () => {
      const mockSetUser = jest.fn()
      const mockRemoveItem = jest.fn()

      initialize({ setUser: mockSetUser })

      fetch.mockResponseOnce('Access Denied', { status: 401 })

      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          removeItem: mockRemoveItem,
        },
      })

      const data = await fetcher()

      expect(data).toEqual({})

      expect(mockSetUser).toHaveBeenCalledWith({})

      expect(mockRemoveItem).toHaveBeenCalledWith(USER)
    })
  })
})
