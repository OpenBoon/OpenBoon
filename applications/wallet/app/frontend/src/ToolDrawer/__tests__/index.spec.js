import TestRenderer, { act } from 'react-test-renderer'

import ToolDrawer from '..'

const noop = () => () => {}

describe('<ToolDrawer />', () => {
  it('should render properly closed', () => {
    const component = TestRenderer.create(
      <ToolDrawer isToolDrawerOpen={false} setToolDrawerOpen={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly opened', () => {
    const component = TestRenderer.create(
      <ToolDrawer isToolDrawerOpen setToolDrawerOpen={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should close when clicking on the overlay', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <ToolDrawer isToolDrawerOpen setToolDrawerOpen={mockFn} />,
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
      <ToolDrawer isToolDrawerOpen setToolDrawerOpen={mockFn} />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Sidebar Menu' })
        .props.onKeyDown({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalledWith(false)
  })
})
