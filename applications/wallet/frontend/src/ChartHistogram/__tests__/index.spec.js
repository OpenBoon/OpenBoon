import TestRenderer, { act } from 'react-test-renderer'

import fields from '../../Filters/__mocks__/fields'

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
      attribute: 'analysis.zvi-face-detection',
      scale: 'absolute',
      values: '10',
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <ChartHistogram chart={chart} chartIndex={0} dispatch={noop} />,
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
                type: 'labelConfidence',
                attribute: 'analysis.zvi-face-detection',
                values: {},
              },
            ]),
          ),
        },
      },
      `/${PROJECT_ID}/visualizer/data-visualization?query=W3sidHlwZSI6ImxhYmVsQ29uZmlkZW5jZSIsImF0dHJpYnV0ZSI6ImFuYWx5c2lzLnp2aS1mYWNlLWRldGVjdGlvbiIsInZhbHVlcyI6e319XQ==`,
    )
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
        .props.onChange({ value: 'analysis.zvi-face-detection' })
    })

    // Select scale
    act(() => {
      component.root
        .findByProps({ label: 'Select the histogram type' })
        .props.onChange({ value: 'relative' })
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
          scale: 'relative',
          attribute: 'analysis.zvi-face-detection',
          values: '5',
        },
      },
    })
  })
})
