import { getNewLabels, cleanup, dispatch, getValues } from '../helpers'

describe('<Filters /> helpers', () => {
  describe('getValues()', () => {
    it('should return the "Limit" value', () => {
      expect(
        getValues({
          type: 'limit',
        }),
      ).toEqual({ maxAssets: 10_000 })
    })
  })

  describe('getNewLabels()', () => {
    it('should remove a label', () => {
      expect(
        getNewLabels({
          labels: ['cat', 'dog'],
          isSelected: true,
          hasModifier: true,
          labelIndex: 1,
          key: 'dog',
        }),
      ).toEqual(['cat'])
    })

    it('should add a label', () => {
      expect(
        getNewLabels({
          labels: ['cat'],
          isSelected: false,
          hasModifier: true,
          labelIndex: 1,
          key: 'dog',
        }),
      ).toEqual(['cat', 'dog'])
    })

    it('should select a unique label', () => {
      expect(
        getNewLabels({
          labels: ['cat', 'dog'],
          isSelected: true,
          hasModifier: false,
          labelIndex: 0,
          key: 'cat',
        }),
      ).toEqual(['cat'])
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

    it('should return an empty array when a label filter has no labels', () => {
      expect(
        cleanup({
          query: btoa(
            JSON.stringify([{ type: 'label', values: { labels: [] } }]),
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

    it('should return an empty array when a label filter is disabled', () => {
      expect(
        cleanup({
          query: btoa(
            JSON.stringify([
              { type: 'label', values: { labels: ['foo'] }, isDisabled: true },
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
                values: {},
                isDisabled: true,
              },
              {
                type: 'range',
                attribute: 'system.filesize',
                values: { min: 1, max: 100 },
                isDisabled: false,
              },
            ]),
          ),
        }),
      ).toEqual(
        btoa(
          JSON.stringify([
            {
              type: 'range',
              attribute: 'system.filesize',
              values: { min: 1, max: 100 },
              isDisabled: false,
            },
          ]),
        ),
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
