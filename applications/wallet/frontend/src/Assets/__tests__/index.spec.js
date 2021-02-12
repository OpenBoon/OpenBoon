import TestRenderer, { act } from 'react-test-renderer'

import Assets from '..'

import assets from '../__mocks__/assets'

import { encode } from '../../Filters/helpers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = assets.results[0].id

jest.mock('../../Visualizer/Navigation', () => 'VisualizerNavigation')
jest.mock('../../Asset/Asset', () => 'AssetAsset')

describe('<Assets />', () => {
  it('should render properly while loading', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRInfiniteResponse({})

    const component = TestRenderer.create(<Assets />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRInfiniteResponse({ data: [assets] })

    const component = TestRenderer.create(<Assets />)

    expect(component.toJSON()).toMatchSnapshot()

    // Zoom in to 4 per row
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.onClick()
    })

    // Zoom in to 1 per row
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.onClick()
    })

    // Attempt to zoom at max size
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.onClick()
    })

    expect(
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.isDisabled,
    ).toBe(true)
    expect(component.toJSON()).toMatchSnapshot()

    // Zoom out to 4 per row
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    // Zoom out to 8 per row
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    // Attempt to zoom out at min size
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    expect(
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.isDisabled,
    ).toBe(true)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render empty properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRInfiniteResponse({
      data: [{ count: 0, results: [] }],
    })

    const component = TestRenderer.create(<Assets />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render empty with filter properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        assetId: ASSET_ID,
        query: encode({
          filters: [
            {
              type: 'similarity',
              attribute: 'analysis.zvi-image-similarity',
              values: { ids: [ASSET_ID] },
            },
          ],
        }),
      },
    })

    require('swr').__setMockUseSWRInfiniteResponse({
      data: [{ count: 0, results: [] }],
    })

    const component = TestRenderer.create(<Assets />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Clear All Filters' })
        .props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}`,
    )
  })

  it('should render empty with no filters properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: {
        projectId: PROJECT_ID,
        assetId: ASSET_ID,
      },
    })

    require('swr').__setMockUseSWRInfiniteResponse({
      data: [{ count: 0, results: [] }],
    })

    const component = TestRenderer.create(<Assets />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
