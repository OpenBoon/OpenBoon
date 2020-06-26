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
      component.root
        .findByProps({ 'aria-label': 'Add Field Filter' })
        .props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer/data-visualization',
        query: {
          projectId: PROJECT_ID,
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
      `/${PROJECT_ID}/visualizer/data-visualization?query=W3sidHlwZSI6ImZhY2V0IiwiYXR0cmlidXRlIjoibG9jYXRpb24uY2l0eSIsInZhbHVlcyI6e319XQ==`,
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

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer/data-visualization',
        query: {
          projectId: PROJECT_ID,
          query: btoa(
            JSON.stringify([
              {
                type: 'facet',
                attribute: 'location.city',
                values: { facets: ['Brooklyn'] },
              },
            ]),
          ),
        },
      },
      `/${PROJECT_ID}/visualizer/data-visualization?query=W3sidHlwZSI6ImZhY2V0IiwiYXR0cmlidXRlIjoibG9jYXRpb24uY2l0eSIsInZhbHVlcyI6eyJmYWNldHMiOlsiQnJvb2tseW4iXX19XQ==`,
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

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer/data-visualization',
        query: {
          projectId: PROJECT_ID,
          query: btoa(
            JSON.stringify([
              {
                type: 'facet',
                attribute: 'location.city',
                values: { facets: ['Brooklyn', 'Zermatt'] },
              },
            ]),
          ),
        },
      },
      `/${PROJECT_ID}/visualizer/data-visualization?query=W3sidHlwZSI6ImZhY2V0IiwiYXR0cmlidXRlIjoibG9jYXRpb24uY2l0eSIsInZhbHVlcyI6eyJmYWNldHMiOlsiQnJvb2tseW4iLCJaZXJtYXR0Il19fV0=`,
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
})
