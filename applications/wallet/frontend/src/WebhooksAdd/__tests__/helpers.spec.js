import { generateSecretKey } from '../helpers'

describe('<WebhooksAdd /> helpers', () => {
  describe('generateSecretKey()', () => {
    it('should generate a new key, slowly', async () => {
      let mockState = { secretKey: 'initial-secret-key' }

      const mockDispatch = jest.fn((value) => {
        mockState = { ...mockState, ...value }
      })

      await generateSecretKey({ state: mockState, dispatch: mockDispatch })()

      expect(mockDispatch).toHaveBeenCalledWith({
        disableSecretKeyButton: true,
      })

      expect(mockDispatch).toHaveBeenCalledTimes(57)

      expect(mockDispatch).toHaveBeenCalledWith({
        disableSecretKeyButton: false,
      })

      expect(mockState.secretKey).toHaveLength(36)
    })
  })
})
