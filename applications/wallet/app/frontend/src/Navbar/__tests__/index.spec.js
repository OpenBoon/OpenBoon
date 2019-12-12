import TestRenderer from 'react-test-renderer'

import Navbar from '..'

describe('<Navbar />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()
    const mockFn2 = jest.fn()
    const mockFn3 = jest.fn()
    const component = TestRenderer.create(
      <Navbar
        projects={[{ id: '1', name: 'project-name', selected: true }]}
        setSelectedProject={mockFn}
        isSidebarOpen
        setSidebarOpen={mockFn2}
        logout={mockFn3}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
