import { getUser } from '../helpers'

describe('<Authentication /> helpers', () => {
  describe('getUser()', () => {
    it('should return no user', () => {
      expect(getUser()).toEqual({})
    })

    it('should return no user', () => {
      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          getItem: () => 'not a json object',
        },
      })

      expect(getUser()).toEqual({})
    })

    it('should return a user', () => {
      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          getItem: () => JSON.stringify({ id: 12345 }),
        },
      })

      expect(getUser()).toEqual({ id: 12345 })
    })
  })
})
