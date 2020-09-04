import TestRenderer, { act } from 'react-test-renderer'

import Timeline from '..'

const noop = () => {}

describe('<Timeline />', () => {
  it('should not render until video has loaded with duration', () => {
    const component = TestRenderer.create(
      <Timeline videoRef={{ current: undefined }} />,
    )

    expect(component.toJSON()).toEqual(null)
  })

  it('should mount and unmount event listeners', () => {
    const mockAddEventListener = jest.fn()
    const mockRemoveEventListener = jest.fn()

    const component = TestRenderer.create(
      <Timeline
        videoRef={{
          current: {
            play: noop,
            pause: noop,
            addEventListener: mockAddEventListener,
            removeEventListener: mockRemoveEventListener,
            currentTime: 0,
            duration: 18,
            paused: true,
          },
        }}
      />,
    )

    // useEffect
    act(() => {})

    expect(mockAddEventListener).toHaveBeenCalled()

    component.unmount()

    expect(mockRemoveEventListener).toHaveBeenCalled()
  })

  it('should play', () => {
    const mockPlay = jest.fn()

    const component = TestRenderer.create(
      <Timeline
        videoRef={{
          current: {
            play: mockPlay,
            pause: noop,
            addEventListener: noop,
            removeEventListener: noop,
            currentTime: 0,
            duration: 18,
            paused: true,
          },
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ 'aria-label': 'Play' }).props.onClick()
    })

    expect(mockPlay).toHaveBeenCalled()
  })

  it('should pause', () => {
    const mockPause = jest.fn()

    const component = TestRenderer.create(
      <Timeline
        videoRef={{
          current: {
            play: noop,
            pause: mockPause,
            addEventListener: noop,
            removeEventListener: noop,
            currentTime: 6,
            duration: 18,
            paused: false,
          },
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ 'aria-label': 'Pause' }).props.onClick()
    })

    expect(mockPause).toHaveBeenCalled()
  })

  it('should seek the previous second', () => {
    const mockPause = jest.fn()

    const current = {
      play: noop,
      pause: mockPause,
      addEventListener: noop,
      removeEventListener: noop,
      currentTime: 6.5,
      duration: 18,
      paused: false,
    }

    const component = TestRenderer.create(<Timeline videoRef={{ current }} />)

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Previous Second' })
        .props.onClick()
    })

    expect(mockPause).toHaveBeenCalled()

    expect(current.currentTime).toBe(5)
  })

  it('should seek the next second', () => {
    const mockPause = jest.fn()

    const current = {
      play: noop,
      pause: mockPause,
      addEventListener: noop,
      removeEventListener: noop,
      currentTime: 6.5,
      duration: 18,
      paused: false,
    }

    const component = TestRenderer.create(<Timeline videoRef={{ current }} />)

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Next Second' })
        .props.onClick()
    })

    expect(mockPause).toHaveBeenCalled()

    expect(current.currentTime).toBe(7)
  })
})
