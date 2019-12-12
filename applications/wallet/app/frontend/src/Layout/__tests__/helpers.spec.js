import { parseId } from '../helpers'

describe('<Layout /> helpers', () => {
  describe('parseId()', () => {
    it('should parseId when receiving url', () => {
      const url = 'http://localhost:3000/api/v1/projects/1'
      const id = parseId({ url })

      expect(id).toEqual('1')
    })
  })
})
