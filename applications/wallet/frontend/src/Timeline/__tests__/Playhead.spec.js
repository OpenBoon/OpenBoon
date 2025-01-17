import TestRenderer, { act } from 'react-test-renderer'

import TimelinePlayhead from '../Playhead'

describe('<TimelinePlayhead />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <TimelinePlayhead
        videoRef={{ current: { currentTime: 5, duration: 10, paused: true } }}
        rulerRef={{
          current: { scrollWidth: 0, scrollLeft: 0, clientWidth: 0 },
        }}
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

  it('should render properly in prod environment', () => {
    require('next/config').__setPublicRuntimeConfig({
      ENVIRONMENT: 'prod',
    })

    const component = TestRenderer.create(
      <TimelinePlayhead
        videoRef={{ current: { currentTime: 5, duration: 10, paused: true } }}
        rulerRef={{
          current: { scrollWidth: 0, scrollLeft: 0, clientWidth: 0 },
        }}
        zoom={100}
        followPlayhead
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
