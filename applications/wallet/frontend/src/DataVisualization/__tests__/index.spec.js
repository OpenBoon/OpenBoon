import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import DataVisualization from '..'

jest.mock('../../Visualizer/Navigation', () => 'VisualizerNavigation')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<DataVisualization />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DataVisualization />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
