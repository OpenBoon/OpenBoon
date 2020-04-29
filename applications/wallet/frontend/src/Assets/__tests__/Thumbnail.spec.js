import TestRenderer from 'react-test-renderer'

import AssetsThumbnail from '../Thumbnail'

import assets from '../__mocks__/assets'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = assets.results[0].id

describe('<AssetsThumbnail />', () => {
  it('should render properly a valid asset', () => {
    require('next/router').__setUseRouter({
      query: {
        projectId: PROJECT_ID,
        filters: '[{"type":"search","value":"Cat"}]',
      },
    })

    const component = TestRenderer.create(
      <AssetsThumbnail asset={assets.results[0]} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly a valid selected asset', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <AssetsThumbnail asset={assets.results[0]} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly an asset without thumbnails', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <AssetsThumbnail asset={assets.results[1]} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
