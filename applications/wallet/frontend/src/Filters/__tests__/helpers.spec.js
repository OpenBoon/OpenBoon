import { formatUrl, cleanup, dispatch } from '../helpers'

describe('<Filters /> helpers', () => {
  describe('formatUrl()', () => {
    it('should return an empty string with no query params', () => {
      expect(formatUrl()).toEqual('')
    })
  })

  describe('cleanup()', () => {
    it('should return an empty array when filters have no value', () => {
      expect(
        cleanup({
          query: btoa(
            JSON.stringify([{ type: 'range', attribute: 'clip.length' }]),
          ),
        }),
      ).toEqual('W10=')
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
