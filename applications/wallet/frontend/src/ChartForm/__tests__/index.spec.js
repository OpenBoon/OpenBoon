import TestRenderer, { act } from 'react-test-renderer'

import fields from '../../Filters/__mocks__/fields'

import ChartForm from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const CHART_ID = '972a8ab5-cdcb-4eea-ada7-f1c88d997fed'

describe('<ChartForm />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
    }

    require('swr').__setMockUseSWRResponse({ data: fields })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <ChartForm chart={chart} chartIndex={0} dispatch={mockDispatch} />,
    )

    // Select attribute
    act(() => {
      component.root
        .findByType('select')
        .props.onChange({ target: { value: 'system.type' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

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
        updatedChart: { id: CHART_ID, type: 'facet', attribute: 'system.type' },
      },
    })
  })

  it('should cancel properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
    }

    require('swr').__setMockUseSWRResponse({ data: fields })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <ChartForm chart={chart} chartIndex={0} dispatch={mockDispatch} />,
    )

    // Cancel chart
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      type: 'DELETE',
      payload: {
        chartIndex: 0,
      },
    })
  })
})
