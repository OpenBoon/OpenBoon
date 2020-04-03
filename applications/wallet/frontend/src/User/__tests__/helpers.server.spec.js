/**
 * @jest-environment node
 */
import { meFetcher } from '../helpers'

describe('<Fetch /> helpers', () => {
  describe('meFetcher()', () => {
    it('should return a noop the server', () => {
      expect(meFetcher()).toBe(undefined)
    })
  })
})
