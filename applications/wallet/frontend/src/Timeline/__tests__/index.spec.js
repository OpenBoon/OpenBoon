import TestRenderer, { act } from 'react-test-renderer'

import Timeline from '..'

const noop = () => {}

jest.mock('../Controls', () => 'TimelineControls')
jest.mock('../../Resizeable', () => 'Resizeable')
jest.mock('../Captions', () => 'TimelineCaptions')
jest.mock('../Playhead', () => 'TimelinePlayhead')
jest.mock('../Detections', () => 'TimelineDetections')

describe('<Timeline />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Timeline videoRef={{ current: undefined }} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should open the Timeline panel', () => {
    const component = TestRenderer.create(
      <Timeline
        videoRef={{
          current: {
            play: noop,
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

    // Open timeline
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open Timeline' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Close timeline
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open Timeline' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
