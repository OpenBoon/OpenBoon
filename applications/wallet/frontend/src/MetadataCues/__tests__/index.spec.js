import TestRenderer from 'react-test-renderer'

import tracks from '../../Asset/__mocks__/tracks'

import MetadataCues, { noop } from '..'

const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

describe('<MetadataCues />', () => {
  it('should render properly without a video', () => {
    const component = TestRenderer.create(
      <MetadataCues videoRef={{ current: undefined }} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a video with a metadata track', () => {
    require('next/router').__setUseRouter({
      query: { assetId: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataCues
        videoRef={{
          current: {
            duration: 42,
            textTracks: {
              ...tracks,
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

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
