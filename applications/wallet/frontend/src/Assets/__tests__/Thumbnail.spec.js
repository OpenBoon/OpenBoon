import TestRenderer, { act } from 'react-test-renderer'

import AssetsThumbnail from '../Thumbnail'

import assets from '../__mocks__/assets'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = assets.results[0].id

describe('<AssetsThumbnail />', () => {
  it('should render properly a valid asset', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        query: btoa(JSON.stringify([{ type: 'search', value: 'Cat' }])),
      },
    })

    const component = TestRenderer.create(
      <AssetsThumbnail asset={assets.results[0]} isActive />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly an invalid asset', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <AssetsThumbnail
        asset={{
          id: 'zs5GXqqMapHkZpKYewLVNPppddQfaCK-',
          metadata: {},
          thumbnailUrl: 'https://dev.console.zvi.zorroa.com/web-proxy.jpg/',
          assetStyle: null,
          videoLength: null,
          videoProxyUrl: null,
        }}
        isActive
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly a video asset', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <AssetsThumbnail asset={assets.results[4]} isActive />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly a valid selected asset', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <AssetsThumbnail asset={assets.results[0]} isActive />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly an asset without thumbnails', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <AssetsThumbnail asset={assets.results[1]} isActive />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should add a new similarity search', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <AssetsThumbnail asset={assets.results[0]} isActive />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Find similar images' })
        .props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          query: btoa(
            JSON.stringify([
              {
                type: 'similarity',
                attribute: 'analysis.zvi-image-similarity',
                values: { ids: [ASSET_ID], minScore: 0.75 },
              },
            ]),
          ),
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6InNpbWlsYXJpdHkiLCJhdHRyaWJ1dGUiOiJhbmFseXNpcy56dmktaW1hZ2Utc2ltaWxhcml0eSIsInZhbHVlcyI6eyJpZHMiOlsicE53blhqVm50Z2JEUWdQWmhrWHFWVC0yVVJNcXZKTkwiXSwibWluU2NvcmUiOjAuNzV9fV0=',
    )
  })

  it('should replace an existing similarity search', () => {
    const mockRouterPush = jest.fn()
    const oldQuery = btoa(
      JSON.stringify([
        {
          type: 'similarity',
          attribute: 'analysis.zvi-image-similarity',
          values: { ids: [ASSET_ID] },
        },
      ]),
    )

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, id: ASSET_ID, query: oldQuery },
    })

    const component = TestRenderer.create(
      <AssetsThumbnail asset={assets.results[1]} isActive />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Find similar images' })
        .props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          query: btoa(
            JSON.stringify([
              {
                type: 'similarity',
                attribute: 'analysis.zvi-image-similarity',
                values: { ids: [assets.results[1].id], minScore: 0.75 },
              },
            ]),
          ),
          id: ASSET_ID,
          projectId: PROJECT_ID,
        },
      },
      `/${PROJECT_ID}/visualizer?id=${ASSET_ID}&query=W3sidHlwZSI6InNpbWlsYXJpdHkiLCJhdHRyaWJ1dGUiOiJhbmFseXNpcy56dmktaW1hZ2Utc2ltaWxhcml0eSIsInZhbHVlcyI6eyJpZHMiOlsiM0REbnVDTnJ1WGlYdFJqS3h3R0p0MlVQR05UQVp1dDQiXSwibWluU2NvcmUiOjAuNzV9fV0=`,
    )
  })
})
