import { getOptions } from '../helpers'

import model from '../../Model/__mocks__/model'

const MODEL_ID = model.id

jest.mock('../../Fetch/helpers', () => ({
  revalidate: jest.fn().mockReturnValueOnce({ results: 'expected return' }),
}))

describe('<AssetLabeling /> helpers', () => {
  describe('getOptions()', () => {
    it('should return empty array if no modelId', async () => {
      const result = await getOptions({ projectId: '', modelId: '' })

      expect(result).toEqual([])
    })

    it('should call revalidate if there is a modelId', async () => {
      const result = await getOptions({ projectId: '', modelId: MODEL_ID })

      expect(result).toEqual('expected return')
    })
  })
})
