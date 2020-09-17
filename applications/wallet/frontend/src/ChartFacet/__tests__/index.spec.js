import TestRenderer, { act } from 'react-test-renderer'

import aggregate from '../__mocks__/aggregate'

import ChartFacet from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const CHART_ID = '972a8ab5-cdcb-4eea-ada7-f1c88d997fed'

jest.mock('../../ChartForm', () => 'ChartForm')

const noop = () => () => {}

describe('<ChartFacet />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
      attribute: 'location.city',
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: aggregate })

    const component = TestRenderer.create(
      <ChartFacet chart={chart} chartIndex={0} dispatch={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ 'aria-label': 'Add Filter' }).props.onClick()
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
      `/[projectId]/visualizer/data-visualization?query=${query}`,
      `/${PROJECT_ID}/visualizer/data-visualization?query=${query}`,
    )
  })

  it('should render properly without data', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
      attribute: 'location.city',
    }

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <ChartFacet chart={chart} chartIndex={0} dispatch={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should add a filter with a value', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
      attribute: 'location.city',
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: aggregate })

    const component = TestRenderer.create(
      <ChartFacet chart={chart} chartIndex={0} dispatch={noop} />,
    )

    act(() => {
      component.root.findByProps({ 'aria-label': 'Brooklyn' }).props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'facet',
          attribute: 'location.city',
          values: { facets: ['Brooklyn'] },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer/data-visualization?query=${query}`,
      `/${PROJECT_ID}/visualizer/data-visualization?query=${query}`,
    )
  })

  it('should add a value to a filter', () => {
    const query = btoa(
      JSON.stringify([
        {
          type: 'facet',
          attribute: 'location.city',
          values: { facets: ['Brooklyn'] },
        },
      ]),
    )

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID, query },
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
      attribute: 'location.city',
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: aggregate })

    const component = TestRenderer.create(
      <ChartFacet chart={chart} chartIndex={0} dispatch={noop} />,
    )

    act(() => {
      component.root.findByProps({ 'aria-label': 'Zermatt' }).props.onClick()
    })

    const newQuery = btoa(
      JSON.stringify([
        {
          type: 'facet',
          attribute: 'location.city',
          values: { facets: ['Brooklyn', 'Zermatt'] },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer/data-visualization?query=${newQuery}`,
      `/${PROJECT_ID}/visualizer/data-visualization?query=${newQuery}`,
    )
  })

  it('should not add a duplicate value', () => {
    const query = btoa(
      JSON.stringify([
        {
          type: 'facet',
          attribute: 'location.city',
          values: { facets: ['Brooklyn'] },
        },
      ]),
    )

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID, query },
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
      attribute: 'location.city',
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: aggregate })

    const component = TestRenderer.create(
      <ChartFacet chart={chart} chartIndex={0} dispatch={noop} />,
    )

    act(() => {
      component.root.findByProps({ 'aria-label': 'Brooklyn' }).props.onClick()
    })

    expect(mockRouterPush).not.toHaveBeenCalled()
  })

  it('should render without an attribute', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
    }

    const component = TestRenderer.create(
      <ChartFacet chart={chart} chartIndex={0} dispatch={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should delete', () => {
    const mockDispatch = jest.fn()

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: aggregate })

    const chart = {
      id: CHART_ID,
      type: 'facet',
      attribute: 'system.type',
    }

    const component = TestRenderer.create(
      <ChartFacet chart={chart} chartIndex={0} dispatch={mockDispatch} />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Delete Chart' })
        .props.onClick()
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      type: 'DELETE',
      payload: {
        chartIndex: 0,
      },
    })
  })

  it('should edit', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: aggregate })

    const chart = {
      id: CHART_ID,
      type: 'facet',
      attribute: 'system.type',
    }

    const component = TestRenderer.create(
      <ChartFacet chart={chart} chartIndex={0} dispatch={noop} />,
    )

    act(() => {
      component.root.findByProps({ 'aria-label': 'Edit Chart' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should add a histogram chart for a labelConfidence compatible attribute', () => {
    const mockDispatch = jest.fn()

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: [{ ...aggregate[0], defaultFilterType: 'labelConfidence' }],
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
      attribute: 'system.type',
    }

    const component = TestRenderer.create(
      <ChartFacet chart={chart} chartIndex={0} dispatch={mockDispatch} />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Add Histogram Visualization' })
        .props.onClick()
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      payload: { attribute: 'system.type', type: 'histogram', values: 10 },
      type: 'CREATE',
    })
  })
})
