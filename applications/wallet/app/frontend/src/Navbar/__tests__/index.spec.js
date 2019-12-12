import TestRenderer from 'react-test-renderer'

import Navbar from '..'

describe('<Navbar />', () => {
  it('should render properly', () => {
    const mockSetSelectedProject = jest.fn()
    const mockSetSidebarOpen = jest.fn()
    const mockLogout = jest.fn()

    const component = TestRenderer.create(
      <Navbar
        projects={[{ id: '1', name: 'project-name', selected: true }]}
        setSelectedProject={mockSetSelectedProject}
        isSidebarOpen
        setSidebarOpen={mockSetSidebarOpen}
        logout={mockLogout}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
