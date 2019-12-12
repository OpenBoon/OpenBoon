import TestRenderer from 'react-test-renderer'

import Navbar from '..'

describe('<Navbar />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()
    const component = TestRenderer.create(
      <Navbar
        projects={[{ id: '1', name: 'project-name', selected: true }]}
        setSelectedProject={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
