import TestRenderer, { act } from 'react-test-renderer'

import aggregate from '../__mocks__/aggregate'

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

    require('swr').__setMockUseSWRResponse({ data: aggregate })

    const component = TestRenderer.create(
      <ChartRange chart={chart} chartIndex={0} dispatch={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ 'aria-label': 'Add Filter' }).props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'range',
          attribute: 'source.filesize',
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

    require('swr').__setMockUseSWRResponse({ data: aggregate })

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

  it('should edit', () => {
    require('swr').__setMockUseSWRResponse({ data: aggregate })

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
      <ChartRange chart={chart} chartIndex={0} dispatch={noop} />,
    )

    act(() => {
      component.root.findByProps({ 'aria-label': 'Edit Chart' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
