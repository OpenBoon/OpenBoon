/**
 * @jest-environment node
 */
import { meFetcher } from '../helpers'

describe('<Fetch /> helpers', () => {
  describe('meFetcher()', () => {
    it('should be a noop on the server', () => {
      expect(meFetcher()).toBe(undefined)
    })
  })
})
