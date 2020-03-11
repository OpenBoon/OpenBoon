import TestRenderer from 'react-test-renderer'

import JobErrorAsset from '../index'
import assets from '../../Assets/__mocks__/assets'

const ASSET = assets.results[0]

describe('<JobErrorAsset />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: ASSET,
    })

    const ASSET_ID = ASSET.id

    const component = TestRenderer.create(<JobErrorAsset assetId={ASSET_ID} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
