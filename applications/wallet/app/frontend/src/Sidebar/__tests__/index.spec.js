import TestRenderer, { act } from 'react-test-renderer'

import Sidebar from '..'

const noop = () => () => {}

describe('<Sidebar />', () => {
  it('should render properly closed', () => {
    const component = TestRenderer.create(
      <Sidebar isSidebarOpen={false} setSidebarOpen={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly opened', () => {
    const component = TestRenderer.create(
      <Sidebar isSidebarOpen setSidebarOpen={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should close when clicking on the overlay', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Sidebar isSidebarOpen setSidebarOpen={mockFn} />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Sidebar Menu' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalledWith(false)
  })

  it('should close when key pressing on the overlay', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Sidebar isSidebarOpen setSidebarOpen={mockFn} />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Sidebar Menu' })
        .props.onKeyDown({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalledWith(false)
  })
})
