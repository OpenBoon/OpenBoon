import TestRenderer, { act } from 'react-test-renderer'

import fields from '../../Filters/__mocks__/fields'

import ChartForm from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const CHART_ID = '972a8ab5-cdcb-4eea-ada7-f1c88d997fed'

const noop = () => () => {}

describe('<ChartForm />', () => {
  it('should render properly without attribute', () => {
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
    const mockSetIsEditing = jest.fn()

    const component = TestRenderer.create(
      <ChartForm
        chart={chart}
        chartIndex={0}
        dispatch={mockDispatch}
        isEditing={false}
        setIsEditing={mockSetIsEditing}
      />,
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
        updatedChart: {
          id: CHART_ID,
          type: 'facet',
          attribute: 'system.type',
          values: '10',
        },
      },
    })
    expect(mockSetIsEditing).toHaveBeenCalledWith(false)
  })

  it('should render properly with attribute', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
      attribute: 'system.type',
    }

    require('swr').__setMockUseSWRResponse({ data: fields })

    const mockDispatch = jest.fn()
    const mockSetIsEditing = jest.fn()

    const component = TestRenderer.create(
      <ChartForm
        chart={chart}
        chartIndex={0}
        dispatch={mockDispatch}
        isEditing
        setIsEditing={mockSetIsEditing}
      />,
    )

    // Select attribute
    act(() => {
      component.root
        .findByType('select')
        .props.onChange({ target: { value: 'clip.pile' } })
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
          type: 'facet',
          attribute: 'clip.pile',
          values: '5',
        },
      },
    })
    expect(mockSetIsEditing).toHaveBeenCalledWith(false)
  })

  it('should cancel properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'range',
    }

    require('swr').__setMockUseSWRResponse({ data: fields })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <ChartForm
        chart={chart}
        chartIndex={0}
        dispatch={mockDispatch}
        isEditing={false}
        setIsEditing={noop}
      />,
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

  it('should cancel properly when editing', () => {
    const mockSetIsEditing = jest.fn()

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const chart = {
      id: CHART_ID,
      type: 'facet',
    }

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(
      <ChartForm
        chart={chart}
        chartIndex={0}
        dispatch={noop}
        isEditing
        setIsEditing={mockSetIsEditing}
      />,
    )

    // Cancel chart
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    expect(mockSetIsEditing).toHaveBeenCalledWith(false)
    expect(component.toJSON()).toMatchSnapshot()
  })
})
