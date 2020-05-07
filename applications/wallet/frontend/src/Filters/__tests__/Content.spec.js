import TestRenderer, { act } from 'react-test-renderer'

import facetAggregate from '../../FilterFacet/__mocks__/aggregate'
import rangeAggregate from '../../FilterRange/__mocks__/aggregate'

import FiltersContent from '../Content'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filters/Reset', () => 'FiltersReset')

describe('<FiltersContent />', () => {
  it('should render the "Exists" filter', () => {
    const filters = [{ attribute: 'location.point', type: 'exists' }]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // click "Missing"
    act(() => {
      component.root
        .findByProps({ children: 'Missing' })
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
                attribute: 'location.point',
                type: 'exists',
                values: { exists: false },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3siYXR0cmlidXRlIjoibG9jYXRpb24ucG9pbnQiLCJ0eXBlIjoiZXhpc3RzIiwidmFsdWVzIjp7ImV4aXN0cyI6ZmFsc2V9fV0=',
    )
  })

  it('should render the "Facet" filter', () => {
    const filters = [{ attribute: 'location.city', type: 'facet', values: {} }]

    require('swr').__setMockUseSWRResponse({ data: facetAggregate })

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    act(() => {
      component.root
        .findAllByProps({ variant: 'NEUTRAL' })[1]
        .props.onClick({ preventDefault: noop })
      component.root
        .findAllByProps({ variant: 'NEUTRAL' })[2]
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render the "Range" filter', () => {
    const {
      results: { max },
    } = rangeAggregate

    const filters = [
      { attribute: 'clip.length', type: 'range', values: { min: 0.1, max } },
    ]

    require('swr').__setMockUseSWRResponse({
      data: {
        ...rangeAggregate,
        results: { ...rangeAggregate.results, min: 0.1 },
      },
    })

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    act(() => {
      component.root.findByProps({ mode: 2 }).props.onUpdate([0.255, max])
      component.root.findByProps({ mode: 2 }).props.onChange([0.255, max])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('FiltersReset').props.onReset()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render the "Label Confidence" filter', () => {
    const filters = [
      {
        attribute: 'analysis.zvi-label-detection',
        type: 'labelConfidence',
        values: {},
      },
    ]

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 604,
        results: {
          docCountErrorUpperBound: 0,
          sumOtherDocCount: 0,
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
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render the "Range" filter with file sizes', () => {
    const filters = [
      { attribute: 'source.filesize', type: 'range', values: {} },
    ]

    require('swr').__setMockUseSWRResponse({ data: rangeAggregate })

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
