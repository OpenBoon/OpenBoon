import TestRenderer from 'react-test-renderer'

import Navbar from '..'

const noop = () => () => {}

describe('<Navbar />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Navbar
        projects={[{ id: '1', name: 'project-name', selected: true }]}
        setSelectedProject={noop}
        isSidebarOpen
        setSidebarOpen={noop}
        logout={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
