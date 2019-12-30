/**
 * @jest-environment node
 */
import { getCsrfToken } from '../helpers'

describe('<Fetch /> helpers', () => {
  describe('getCsrfToken()', () => {
    it('should return an empty string on the server', () => {
      expect(getCsrfToken()).toBe('')
    })
  })
})
