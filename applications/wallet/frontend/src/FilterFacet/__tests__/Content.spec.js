import TestRenderer, { act } from 'react-test-renderer'

import FilterFacetContent, { noop } from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filter/Reset', () => 'FilterReset')

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
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Search
    act(() => {
      component.root
        .findByProps({ type: 'search' })
        .props.onChange({ target: { value: 'boro' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Select
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Tyngsboro' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'facet',
          attribute: 'location.city',
          values: { facets: ['Tyngsboro'] },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
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
        pathname="/[projectId]/visualizer"
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

    const query = btoa(
      JSON.stringify([
        {
          type: 'facet',
          attribute: 'location.city',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should render with no data', () => {
    const filter = {
      attribute: 'location.city',
      type: 'facet',
      values: {},
    }

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <FilterFacetContent
        pathname="/[projectId]/visualizer"
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
