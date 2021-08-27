import { onSave } from '../helpers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

describe('<BulkAssetLabeling /> helpers', () => {
  describe('onSave()', () => {
    it('should redirect properly', async () => {
      const mockDispatch = jest.fn()
      const mockRouterPush = jest.fn()

      require('next/router').__setMockPushFunction(mockRouterPush)

      await onSave({
        projectId: PROJECT_ID,
        query: '',
        state: {
          datasetId: DATASET_ID,
          labels: {},
          lastLabel: 'cat',
          lastScope: 'TEST',
        },
        dispatch: mockDispatch,
      })

      expect(mockDispatch).toHaveBeenCalled()

      expect(mockRouterPush).toHaveBeenCalledWith(
        '/[projectId]/visualizer?action=bulk-labeling-success',
        `/${PROJECT_ID}/visualizer`,
      )
    })
  })
})
