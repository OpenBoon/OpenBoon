import TestRenderer from 'react-test-renderer'

import JobErrorAsset from '../index'
import asset from '../../Asset/__mocks__/asset'

const ASSET_ID = asset.id

describe('<JobErrorAsset />', () => {
  it('should render properly when asset has proxies', () => {
    require('swr').__setMockUseSWRResponse({ data: asset })

    const component = TestRenderer.create(<JobErrorAsset assetId={ASSET_ID} />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when asset has no proxies', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...asset, metadata: { ...asset.metadata, files: [] } },
    })

    const component = TestRenderer.create(<JobErrorAsset assetId={ASSET_ID} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
