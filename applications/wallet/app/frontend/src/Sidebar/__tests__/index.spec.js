import TestRenderer, { act } from 'react-test-renderer'

import Sidebar from '..'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Sidebar />', () => {
  it('should render properly closed', () => {
    const component = TestRenderer.create(
      <Sidebar isSidebarOpen={false} setSidebarOpen={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly opened', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

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

  it('should close the sidebar on route change', () => {
    const mockOnFunction = jest.fn((_, callback) => {
      // invoke callbacks immediately instead of queuing them
      callback()
    })
    const mockSetSidebarOpen = jest.fn()
    const mockOffFunction = jest.fn()

    require('next/router').__setMockOnFunction(mockOnFunction)
    require('next/router').__setMockOffFunction(mockOffFunction)

    const component = TestRenderer.create(
      <Sidebar isSidebarOpen setSidebarOpen={mockSetSidebarOpen} />,
    )

    // useEffect
    act(() => {})

    expect(mockOnFunction).toHaveBeenCalled()
    expect(mockSetSidebarOpen).toHaveBeenCalled()

    component.unmount()

    expect(mockOffFunction).toHaveBeenCalled()
  })
})
