import TestRenderer, { act } from 'react-test-renderer'

import AssetsLightbox from '../Lightbox'

import assets from '../__mocks__/assets'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Asset/Asset', () => 'AssetAsset')

describe('<AssetsLightbox />', () => {
  it('should render properly', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: assets.results[2].id },
    })

    const component = TestRenderer.create(
      <AssetsLightbox assets={assets.results} columnCount={2} />,
    )

    // useEffect
    act(() => {})

    // Open Lightbox
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'Space' })

      document.dispatchEvent(event)
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Left Arrow
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'ArrowLeft' })

      document.dispatchEvent(event)
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: PROJECT_ID,
          id: assets.results[1].id,
        },
      },
      `/${PROJECT_ID}/visualizer?id=${assets.results[1].id}`,
    )

    // Right Arrow
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'ArrowRight' })

      document.dispatchEvent(event)
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: PROJECT_ID,
          id: assets.results[3].id,
        },
      },
      `/${PROJECT_ID}/visualizer?id=${assets.results[3].id}`,
    )

    // Up Arrow
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'ArrowUp' })

      document.dispatchEvent(event)
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: PROJECT_ID,
          id: assets.results[0].id,
        },
      },
      `/${PROJECT_ID}/visualizer?id=${assets.results[0].id}`,
    )

    // Down Arrow
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'ArrowDown' })

      document.dispatchEvent(event)
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: PROJECT_ID,
          id: assets.results[4].id,
        },
      },
      `/${PROJECT_ID}/visualizer?id=${assets.results[4].id}`,
    )

    // Close Lightbox
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'Escape' })

      document.dispatchEvent(event)
    })

    expect(component.toJSON()).toEqual(null)
  })

  it('should do nothing if no asset is selected', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <AssetsLightbox assets={assets.results} columnCount={2} />,
    )

    // useEffect
    act(() => {})

    // Press Space
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'Space' })

      document.dispatchEvent(event)
    })

    expect(component.toJSON()).toEqual(null)
  })

  it('should close when clicking the button', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: assets.results[2].id },
    })

    const component = TestRenderer.create(
      <AssetsLightbox assets={assets.results} columnCount={2} />,
    )

    // useEffect
    act(() => {})

    // Open Lightbox
    act(() => {
      const event = new KeyboardEvent('keydown', { code: 'Space' })

      document.dispatchEvent(event)
    })

    act(() => {
      component.root.findByProps({ 'aria-label': 'Close' }).props.onClick({})
    })

    expect(component.toJSON()).toEqual(null)
  })
})
