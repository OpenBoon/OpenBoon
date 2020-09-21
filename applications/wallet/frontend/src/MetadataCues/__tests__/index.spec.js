import TestRenderer from 'react-test-renderer'

import MetadataCues from '..'

const noop = () => {}

const TRACKS = [
  {
    label: 'English',
    kind: 'captions',
    src: '/webvtt/english.vtt',
    mode: 'disabled',
  },
  {
    label: 'French',
    kind: 'captions',
    src: '/webvtt/french.vtt',
    mode: 'disabled',
  },
  {
    label: 'metadata',
    kind: 'metadata',
    src: '/webvtt/metadata.vtt',
    mode: 'disabled',
  },
]

describe('<MetadataCues />', () => {
  it('should render nothing without a video', () => {
    const component = TestRenderer.create(
      <MetadataCues videoRef={{ current: undefined }} />,
    )

    expect(component.toJSON()).toBe(null)
  })

  it('should render nothing with a video without metadata track', () => {
    const component = TestRenderer.create(
      <MetadataCues
        videoRef={{
          current: {
            duration: 42,
            textTracks: {
              0: TRACKS[0],
              1: TRACKS[1],
              length: 2,
              addEventListener: noop,
              removeEventListener: noop,
            },
          },
        }}
      />,
    )

    expect(component.toJSON()).toBe(null)
  })

  it('should render properly with a video with a metadata track', () => {
    const component = TestRenderer.create(
      <MetadataCues
        videoRef={{
          current: {
            duration: 42,
            textTracks: {
              ...TRACKS,
              length: 3,
              addEventListener: noop,
              removeEventListener: noop,
            },
          },
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
