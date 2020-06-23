import TestRenderer, { act } from 'react-test-renderer'

import ChartRange from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const CHART_ID = '972a8ab5-cdcb-4eea-ada7-f1c88d997fed'

jest.mock('../../ChartForm', () => 'ChartForm')

const noop = () => () => {}

describe('<ChartRange />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'range',
      attribute: 'source.filesize',
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 584,
        results: {
          count: 580,
          min: 180,
          max: 8525.0,
          avg: 899.5689655172414,
          sum: 521750.0,
        },
      },
    })

    const component = TestRenderer.create(
      <ChartRange chart={chart} chartIndex={0} dispatch={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ 'aria-label': 'Add Filter' }).props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer/data-visualization',
        query: {
          projectId: PROJECT_ID,
          query: btoa(
            JSON.stringify([
              {
                type: 'range',
                attribute: 'source.filesize',
                values: {},
              },
            ]),
          ),
        },
      },
      `/${PROJECT_ID}/visualizer/data-visualization?query=W3sidHlwZSI6InJhbmdlIiwiYXR0cmlidXRlIjoic291cmNlLmZpbGVzaXplIiwidmFsdWVzIjp7fX1d`,
    )
  })

  it('should render properly without data', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'range',
      attribute: 'source.filesize',
    }

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <ChartRange chart={chart} chartIndex={0} dispatch={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not add a duplicate filter', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: {
        projectId: PROJECT_ID,
        query: btoa(
          JSON.stringify([
            {
              type: 'range',
              attribute: 'source.filesize',
              values: {},
            },
          ]),
        ),
      },
    })

    const chart = {
      id: CHART_ID,
      type: 'range',
      attribute: 'source.filesize',
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 584,
        results: {
          count: 580,
          min: 180,
          max: 8525.0,
          avg: 899.5689655172414,
          sum: 521750.0,
        },
      },
    })

    const component = TestRenderer.create(
      <ChartRange chart={chart} chartIndex={0} dispatch={noop} />,
    )

    act(() => {
      component.root.findByProps({ 'aria-label': 'Add Filter' }).props.onClick()
    })

    expect(mockRouterPush).not.toHaveBeenCalled()
  })

  it('should not render without an attribute', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'range',
    }

    const component = TestRenderer.create(
      <ChartRange chart={chart} chartIndex={0} dispatch={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
