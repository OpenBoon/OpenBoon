import TestRenderer from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'
import project from '../../Project/__mocks__/project'

import AssetLabelingHeader from '../Header'

const PROJECT_ID = project.id
const ASSET_ID = asset.id

describe('<AssetLabelingHeader />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: asset })

    const component = TestRenderer.create(
      <AssetLabelingHeader projectId={PROJECT_ID} assetId={ASSET_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
