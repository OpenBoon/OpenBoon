import TestRenderer, { act } from 'react-test-renderer'

import VisualizerPanel from '../Panel'

jest.mock('../../Resizeable', () => 'Resizeable')

describe('<VisualizerPanel />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<VisualizerPanel />)

    expect(component.toJSON()).toMatchSnapshot()

    // Open Panel
    act(() => {
      component.root.findByProps({ 'aria-label': 'Filters' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Resize large
    act(() => {
      component.root.findByType('Resizeable').props.onMouseUp({ width: 500 })
    })

    // Resize to close
    act(() => {
      component.root.findByType('Resizeable').props.onMouseUp({ width: 100 })
    })

    // Open Panel
    act(() => {
      component.root.findByProps({ 'aria-label': 'Filters' }).props.onClick()
    })

    // Close Panel
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Panel' })
        .props.onClick()
    })
  })
})
