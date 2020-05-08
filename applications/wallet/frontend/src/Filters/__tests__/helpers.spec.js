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

    it('should return an empty array when filters are disabled', () => {
      expect(
        cleanup({
          query: btoa(
            JSON.stringify([
              { type: 'range', attribute: 'clip.length', isDisabled: true },
            ]),
          ),
        }),
      ).toEqual('W10=')
    })

    it('should clean up disabled filters', () => {
      expect(
        cleanup({
          query: btoa(
            JSON.stringify([
              {
                type: 'range',
                attribute: 'clip.length',
                value: {},
                isDisabled: true,
              },
              {
                type: 'range',
                attribute: 'system.filesize',
                value: { min: 1, max: 100 },
                isDisabled: false,
              },
            ]),
          ),
        }),
      ).toEqual(
        'W3sidHlwZSI6InJhbmdlIiwiYXR0cmlidXRlIjoiY2xpcC5sZW5ndGgiLCJ2YWx1ZSI6e30sImlzRGlzYWJsZWQiOnRydWV9LHsidHlwZSI6InJhbmdlIiwiYXR0cmlidXRlIjoic3lzdGVtLmZpbGVzaXplIiwidmFsdWUiOnt9LCJpc0Rpc2FibGVkIjpmYWxzZX1d',
      )
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
