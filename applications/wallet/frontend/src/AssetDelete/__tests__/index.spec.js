import TestRenderer, { act } from 'react-test-renderer'

import Delete from '..'

import asset from '../../Asset/__mocks__/asset'

const ASSET_ID = asset.id
const PROJECT_ID = asset.metadata.system.projectId

describe('<Delete />', () => {
  it('should render properly when no asset is selected', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: {},
    })

    const component = TestRenderer.create(<Delete />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when as asset is selected', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: asset })

    const mockRouterPush = jest.fn()
    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(<Delete />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Delete Asset' })
        .props.onClick()
    })

    act(() => {
      component.root.findByProps({ 'aria-label': 'Cancel' }).props.onClick()
    })

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Delete Asset' })
        .props.onClick()
    })

    // Mock Failure
    fetch.mockResponseOnce('', { status: 400 })

    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Confirm Delete Asset' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Success
    fetch.mockResponseOnce('{}')

    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Confirm Delete Asset' })
        .props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: { id: '', action: 'delete-asset-success' },
      },
      `/${PROJECT_ID}/visualizer?action=delete-asset-success`,
    )
  })

  it('should render properly when asset is deleted', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { action: 'delete-asset-success' },
    })

    const component = TestRenderer.create(<Delete />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should do nothing', () => {
    const mockFn = jest.fn()
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: asset })

    const component = TestRenderer.create(<Delete />)

    act(() => {
      component.root
        .findByType('form')
        .props.onSubmit({ preventDefault: mockFn })
    })

    expect(mockFn).toHaveBeenCalled()
  })
})
