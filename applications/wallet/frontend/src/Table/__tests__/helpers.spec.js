import { onRowClickRouterPush } from '../helpers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Table /> helpers', () => {
  describe('onRowClickRouterPush()', () => {
    it('should navigate on a click on the row directly', async () => {
      const mockRouterPush = jest.fn()

      require('next/router').__setMockPushFunction(mockRouterPush)

      onRowClickRouterPush('/[projectId]', `/${PROJECT_ID}`)()

      expect(mockRouterPush).toHaveBeenCalledWith(
        '/[projectId]',
        `/${PROJECT_ID}`,
      )
    })

    it('should not navigate on a click on a link', async () => {
      const mockRouterPush = jest.fn()

      require('next/router').__setMockPushFunction(mockRouterPush)

      onRowClickRouterPush(
        '/[projectId]',
        `/${PROJECT_ID}`,
      )({ target: { localName: 'a' } })

      expect(mockRouterPush).not.toHaveBeenCalled()
    })
  })
})
