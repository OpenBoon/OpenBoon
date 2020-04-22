import { formatUrl, dispatch } from '../helpers'

describe('<Filters /> helpers', () => {
  describe('formatUrl()', () => {
    it('should return an empty string with no query params', () => {
      expect(formatUrl()).toEqual('')
    })
  })

  describe('dispatch()', () => {
    it('should do nothing', () => {
      const mockRouterPush = jest.fn()

      require('next/router').__setMockPushFunction(mockRouterPush)

      dispatch({})

      expect(mockRouterPush).not.toHaveBeenCalled()
    })
  })
})
