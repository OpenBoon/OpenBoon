import TestRenderer, { act } from 'react-test-renderer'

import FilterLabelConfidenceContent, { noop } from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filters/Reset', () => 'FiltersReset')

describe('<FilterLabelConfidenceContent />', () => {
  it('should select a label', () => {
    const filter = {
      type: 'labelConfidence',
      attribute: 'analysis.zvi-label-detection',
      values: {},
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [
            { key: 'web_site', docCount: 134 },
            { key: 'alp', docCount: 75 },
            { key: 'sports_car', docCount: 56 },
            { key: 'menu', docCount: 45 },
          ],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterLabelConfidenceContent
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
        .findByProps({ 'aria-label': 'web_site' })
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
                type: 'labelConfidence',
                attribute: 'analysis.zvi-label-detection',
                values: { labels: ['web_site'], min: 0, max: 1 },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImxhYmVsQ29uZmlkZW5jZSIsImF0dHJpYnV0ZSI6ImFuYWx5c2lzLnp2aS1sYWJlbC1kZXRlY3Rpb24iLCJ2YWx1ZXMiOnsibGFiZWxzIjpbIndlYl9zaXRlIl0sIm1pbiI6MCwibWF4IjoxfX1d',
    )
  })

  it('should unselect a label', () => {
    const filter = {
      type: 'labelConfidence',
      attribute: 'analysis.zvi-label-detection',
      values: { labels: ['web_site'], min: 0.0, max: 1.0 },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [
            { key: 'web_site' },
            { key: 'alp' },
            { key: 'sports_car' },
            { key: 'menu' },
          ],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterLabelConfidenceContent
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
        .findByProps({ 'aria-label': 'web_site' })
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
                type: 'labelConfidence',
                attribute: 'analysis.zvi-label-detection',
                values: {},
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImxhYmVsQ29uZmlkZW5jZSIsImF0dHJpYnV0ZSI6ImFuYWx5c2lzLnp2aS1sYWJlbC1kZXRlY3Rpb24iLCJ2YWx1ZXMiOnt9fV0=',
    )
  })

  it('should render with no buckets', () => {
    const filter = {
      type: 'labelConfidence',
      attribute: 'analysis.zvi-label-detection',
      values: { labels: [], min: 0.0, max: 1.0 },
    }

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterLabelConfidenceContent
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
