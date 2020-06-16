import TestRenderer from 'react-test-renderer'

import VisualizerNavigation from '../Navigation'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<VisualizerNavigation />', () => {
  it('should render properly for the Assets', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <VisualizerNavigation itemCount={42} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for the Data Visualization', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer/data-visualization',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <VisualizerNavigation itemCount={42} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
