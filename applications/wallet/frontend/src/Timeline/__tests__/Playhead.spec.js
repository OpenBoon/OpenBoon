import TestRenderer, { act } from 'react-test-renderer'

import TimelinePlayhead from '../Playhead'

describe('<TimelinePlayhead />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <TimelinePlayhead
        videoRef={{ current: { currentTime: 5, duration: 10, paused: true } }}
        rulerRef={{ current: { scrollLeft: 0 } }}
        zoom={100}
        followPlayhead
      />,
    )

    // useEffect
    act(() => {})

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.unmount()
    })
  })
})
