import TestRenderer, { act } from 'react-test-renderer'

import tracks from '../../Asset/__mocks__/tracks'

import TimelineMetadata from '../Metadata'

const noop = () => {}

const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

describe('<TimelineMetadata />', () => {
  it('should toggle Metadata', () => {
    const component = TestRenderer.create(
      <TimelineMetadata
        videoRef={{
          current: {
            textTracks: {
              0: tracks[2],
              length: 2,
              addEventListener: noop,
              removeEventListener: noop,
            },
          },
        }}
        assetId={ASSET_ID}
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open Metadata' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Metadata' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
