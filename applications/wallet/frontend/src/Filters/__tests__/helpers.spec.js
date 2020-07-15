import { getNewFacets, cleanup, dispatch } from '../helpers'

describe('<Filters /> helpers', () => {
  describe('getNewFacets()', () => {
    it('should remove a facet', () => {
      expect(
        getNewFacets({
          facets: ['cat', 'dog'],
          isSelected: true,
          hasModifier: true,
          facetIndex: 1,
          key: 'dog',
        }),
      ).toEqual(['cat'])
    })

    it('should add a facet', () => {
      expect(
        getNewFacets({
          facets: ['cat'],
          isSelected: false,
          hasModifier: true,
          facetIndex: 1,
          key: 'dog',
        }),
      ).toEqual(['cat', 'dog'])
    })

    it('should select a unique facet', () => {
      expect(
        getNewFacets({
          facets: ['cat', 'dog'],
          isSelected: true,
          hasModifier: false,
          facetIndex: 0,
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
