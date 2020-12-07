import TestRenderer from 'react-test-renderer'

import videoAsset from '../__mocks__/videoAsset'

import AssetAsset from '../Asset'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'srL8ob5cTpCJjYoKkqqfa2ciyG425dGi'

describe('<AssetAsset />', () => {
  it('should render properly in QuickView', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        ...videoAsset,
        signedUrl: {
          uri: 'https://storage.googleapis.com/video.mp4',
          mediaType: 'video/mp4',
        },
      },
    })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(<AssetAsset isQuickView />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
