import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import assets from '../../Assets/__mocks__/assets'

import User from '../../User'

import DataVisualization from '..'

jest.mock('../../Visualizer/Navigation', () => 'VisualizerNavigation')
jest.mock('../../ChartForm', () => 'ChartForm')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<DataVisualization />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: assets })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DataVisualization />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Add Visualization
    act(() => {
      component.root.findAllByProps({ children: 'Create' })[0].props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Click "Add Chart"
    act(() => {
      component.root.findByProps({ 'aria-label': 'Add Chart' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Cancel
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    // Click "Delete All"
    act(() => {
      component.root.findByProps({ children: 'Delete All' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
