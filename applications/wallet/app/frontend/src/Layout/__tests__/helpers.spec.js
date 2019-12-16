import { getJobId } from '../helpers'

describe('<Layout /> helpers', () => {
  describe('getJobId()', () => {
    it('should getJobId when receiving url', () => {
      const url = 'http://localhost:3000/api/v1/projects/1'
      const id = getJobId({ url })

      expect(id).toEqual('1')
    })
  })
})
