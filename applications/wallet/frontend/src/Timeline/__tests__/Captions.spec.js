import TestRenderer, { act } from 'react-test-renderer'

import tracks from '../../Asset/__mocks__/tracks'

import TimelineCaptions from '../Captions'

const noop = () => {}

describe('<TimelineCaptions />', () => {
  it('should return null in the absence of tracks', () => {
    const component = TestRenderer.create(
      <TimelineCaptions
        videoRef={{ current: undefined }}
        initialTrackIndex={-1}
      />,
    )

    expect(component.toJSON()).toBe(null)
  })

  it('should return null in the absence of caption tracks', () => {
    const metadataTrack = { ...tracks[2] }

    const component = TestRenderer.create(
      <TimelineCaptions
        videoRef={{
          current: {
            textTracks: {
              0: metadataTrack,
              length: 1,
              addEventListener: noop,
              removeEventListener: noop,
            },
          },
        }}
        initialTrackIndex={-1}
      />,
    )

    expect(component.toJSON()).toBe(null)
  })

  it('should mount and unmount event listeners', () => {
    const mockAddEventListener = jest.fn()
    const mockRemoveEventListener = jest.fn()

    const component = TestRenderer.create(
      <TimelineCaptions
        videoRef={{
          current: {
            textTracks: {
              ...tracks,
              length: 3,
              addEventListener: mockAddEventListener,
              removeEventListener: mockRemoveEventListener,
            },
          },
        }}
        initialTrackIndex={-1}
      />,
    )

    // useEffect
    act(() => {})

    expect(mockAddEventListener).toHaveBeenCalled()

    act(() => {
      component.unmount()
    })

    expect(mockRemoveEventListener).toHaveBeenCalled()
  })

  it('should render properly with no track enabled', () => {
    const component = TestRenderer.create(
      <TimelineCaptions
        videoRef={{
          current: {
            textTracks: {
              ...tracks,
              addEventListener: noop,
              removeEventListener: noop,
            },
          },
        }}
        initialTrackIndex={-1}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a track enabled', () => {
    const component = TestRenderer.create(
      <TimelineCaptions
        videoRef={{
          current: {
            textTracks: {
              ...tracks,
              length: 3,
              addEventListener: noop,
              removeEventListener: noop,
            },
          },
        }}
        initialTrackIndex={1}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should enable the first available track', () => {
    const englishTrack = { ...tracks[0] }

    const component = TestRenderer.create(
      <TimelineCaptions
        videoRef={{
          current: {
            textTracks: {
              0: englishTrack,
              length: 1,
              addEventListener: noop,
              removeEventListener: noop,
            },
          },
        }}
        initialTrackIndex={-1}
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Enable Captions' })
        .props.onClick()
    })

    expect(englishTrack.mode).toBe('showing')
  })

  it('should disable any track', () => {
    const englishTrack = { ...tracks[0], mode: 'showing' }

    const component = TestRenderer.create(
      <TimelineCaptions
        videoRef={{
          current: {
            textTracks: {
              0: englishTrack,
              length: 1,
              addEventListener: noop,
              removeEventListener: noop,
            },
          },
        }}
        initialTrackIndex={0}
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Disable Captions' })
        .props.onClick()
    })

    expect(englishTrack.mode).toBe('disabled')
  })

  it('should toggle any track', () => {
    const englishTrack = { ...tracks[0], mode: 'showing' }
    const frenchTrack = { ...tracks[1] }

    const component = TestRenderer.create(
      <TimelineCaptions
        videoRef={{
          current: {
            textTracks: {
              0: englishTrack,
              1: frenchTrack,
              length: 2,
              addEventListener: noop,
              removeEventListener: noop,
            },
          },
        }}
        initialTrackIndex={0}
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Captions Menu' })
        .props.onClick()
    })

    act(() => {
      component.root.findByProps({ children: 'French' }).props.onClick({})
    })

    expect(englishTrack.mode).toBe('disabled')

    expect(frenchTrack.mode).toBe('showing')
  })
})
