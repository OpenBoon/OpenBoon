import TestRenderer, { act } from 'react-test-renderer'

import fields from '../../Filters/__mocks__/fields'
import aggregate from '../__mocks__/aggregate'

import ChartHistogram from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const CHART_ID = '972a8ab5-cdcb-4eea-ada7-f1c88d997fed'

const noop = () => () => {}

describe('<ChartHistogram />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'histogram',
      attribute: 'analysis.boonai-face-detection',
      values: '10',
    }

    require('swr').__setMockUseSWRResponse({ data: aggregate })

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <ChartHistogram chart={chart} chartIndex={0} dispatch={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ 'aria-label': 'Add Filter' }).props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'labelConfidence',
          attribute: 'analysis.boonai-face-detection',
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
      type: 'histogram',
      attribute: 'analysis.boonai-face-detection',
      values: '10',
    }

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <ChartHistogram chart={chart} chartIndex={0} dispatch={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly without an attribute', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'histogram',
    }

    require('swr').__setMockUseSWRResponse({ data: fields })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <ChartHistogram chart={chart} chartIndex={0} dispatch={mockDispatch} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Select attribute
    act(() => {
      component.root
        .findByProps({ label: 'Metadata Type' })
        .props.onChange({ value: 'analysis.boonai-face-detection' })
    })

    // Set Values
    act(() => {
      component.root
        .findByProps({ type: 'number' })
        .props.onChange({ target: { value: '5' } })
    })

    // Save chart
    act(() => {
      component.root
        .findByProps({ children: 'Save Visualization' })
        .props.onClick()
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      type: 'UPDATE',
      payload: {
        chartIndex: 0,
        updatedChart: {
          id: CHART_ID,
          type: 'histogram',
          attribute: 'analysis.boonai-face-detection',
          values: '5',
        },
      },
    })
  })

  it('should add a facet chart', () => {
    const mockDispatch = jest.fn()

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: aggregate })

    const chart = {
      id: CHART_ID,
      type: 'histogram',
      attribute: 'analysis.boonai-face-detection',
      values: '10',
    }

    const component = TestRenderer.create(
      <ChartHistogram chart={chart} chartIndex={0} dispatch={mockDispatch} />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Add Facet Visualization' })
        .props.onClick()
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      payload: {
        attribute: 'analysis.boonai-face-detection',
        type: 'facet',
        values: 10,
      },
      type: 'CREATE',
    })
  })
})
