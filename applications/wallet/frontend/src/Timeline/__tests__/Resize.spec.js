import TestRenderer, { act } from 'react-test-renderer'

import TimelineResize from '../Resize'

const noop = () => () => {}

describe('<TimelineResize />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <TimelineResize
        dispatch={noop}
        zoom={100}
        videoRef={{
          current: { duration: 18, currentTime: 0 },
        }}
        rulerRef={{
          current: { scrollLeft: 0, scrollWidth: 100 },
        }}
      />,
    )

    act(() => {})

    // Zoom in to 200%
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.onClick()
    })

    // Zoom out to 100%
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    // Attempt to zoom out further than minimum zoom of 100%
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    expect(
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.isDisabled,
    ).toBe(true)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when ref values are null', () => {
    const component = TestRenderer.create(
      <TimelineResize
        dispatch={noop}
        zoom={100}
        videoRef={{ current: null }}
        rulerRef={{ current: null }}
      />,
    )

    act(() => {})

    // Zoom in to 200%
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.onClick()
    })

    // Zoom out to 100%
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    // Attempt to zoom out further than minimum zoom of 100%
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    expect(
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.isDisabled,
    ).toBe(true)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
