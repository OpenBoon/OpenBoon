import TestRenderer, { act } from 'react-test-renderer'

import FilterFacetContent, { noop } from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filters/Reset', () => 'FiltersReset')

describe('<FilterFacetContent />', () => {
  it('should select a facet', () => {
    const filter = {
      attribute: 'location.city',
      type: 'facet',
      values: { facets: [] },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [
            { key: 'Tyngsboro' },
            { key: 'Brooklyn' },
            { key: 'Cát Bà' },
          ],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterFacetContent
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Tyngsboro' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: btoa(
            JSON.stringify([
              {
                type: 'facet',
                attribute: 'location.city',
                values: { facets: ['Tyngsboro'] },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImZhY2V0IiwiYXR0cmlidXRlIjoibG9jYXRpb24uY2l0eSIsInZhbHVlcyI6eyJmYWNldHMiOlsiVHluZ3Nib3JvIl19fV0=',
    )
  })

  it('should unselect a facet', () => {
    const filter = {
      attribute: 'location.city',
      type: 'facet',
      values: { facets: ['Tyngsboro'] },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [
            { key: 'Tyngsboro' },
            { key: 'Brooklyn' },
            { key: 'Cát Bà' },
          ],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterFacetContent
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Tyngsboro' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: btoa(
            JSON.stringify([
              {
                type: 'facet',
                attribute: 'location.city',
                values: {},
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImZhY2V0IiwiYXR0cmlidXRlIjoibG9jYXRpb24uY2l0eSIsInZhbHVlcyI6e319XQ==',
    )
  })

  it('should render with no buckets', () => {
    const filter = {
      attribute: 'location.city',
      type: 'facet',
      values: {},
    }

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterFacetContent
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
