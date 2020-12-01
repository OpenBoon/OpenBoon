import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'
import models from '../../Models/__mocks__/models'

import AssetLabeling from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
const MODEL_ID = '621bf774-89d9-1244-9596-d6df43f1ede5'

jest.mock('../Add', () => 'AssetLabelingAdd')

const noop = () => () => {}

describe('<AssetLabeling />', () => {
  it('should render properly with no selected asset', () => {
    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render with a selected asset', async () => {
    require('swr').__setMockUseSWRResponse({ data: { ...models, ...asset } })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    // Click Accordion header link
    act(() => {
      component.root
        .findByProps({ children: 'Create New Model' })
        .props.onClick({ stopPropagation: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no labels', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...models, metadata: { source: asset.metadata.source } },
    })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with missing model', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...asset, results: [] },
    })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should edit labels', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: { ...models, ...asset } })

    const component = TestRenderer.create(<AssetLabeling />)

    // eslint-disable-next-line no-proto
    const spy = jest.spyOn(localStorage.__proto__, 'setItem')

    // Open first label's kebab menu
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Toggle Actions Menu' })[0]
        .props.onClick()
    })

    // Click Edit Label
    act(() => {
      component.root.findByProps({ children: 'Edit Label' }).props.onClick()
    })

    expect(spy).toHaveBeenCalledWith(
      `AssetLabelingAdd.${PROJECT_ID}`,
      `{"modelId":"${MODEL_ID}","label":"Space","scope":"TRAIN","assetId":"${ASSET_ID}"}`,
    )

    spy.mockClear()
  })

  it('should delete labels', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: { ...models, ...asset } })

    const component = TestRenderer.create(<AssetLabeling />)

    // Open first label's kebab menu
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Toggle Actions Menu' })[0]
        .props.onClick()
    })

    // Click Edit Label
    act(() => {
      component.root.findByProps({ children: 'Edit Label' }).props.onClick()
    })

    // Re-open first label's kebab menu
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Toggle Actions Menu' })[0]
        .props.onClick()
    })

    // Click Delete Label
    act(() => {
      component.root.findByProps({ children: 'Delete Label' }).props.onClick()
    })

    // Click Cancel button in modal
    act(() => {
      component.root.findByProps({ title: 'Delete Label' }).props.onCancel()
    })

    // Re-open first label's kebab menu
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Toggle Actions Menu' })[0]
        .props.onClick()
    })

    // Click Delete Label
    act(() => {
      component.root.findByProps({ children: 'Delete Label' }).props.onClick()
    })

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Delete button in modal
    await act(async () => {
      component.root.findByProps({ title: 'Delete Label' }).props.onConfirm()
    })

    // Re-open first label's kebab menu
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Toggle Actions Menu' })[0]
        .props.onClick()
    })

    // Click Delete Label
    act(() => {
      component.root.findByProps({ children: 'Delete Label' }).props.onClick()
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Label Deleted' }))

    // Click Delete button in modal
    await act(async () => {
      component.root.findByProps({ title: 'Delete Label' }).props.onConfirm()
    })

    expect(fetch.mock.calls.length).toEqual(2)

    // Call from Delete Modal
    expect(fetch.mock.calls[1][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/delete_labels/`,
    )

    expect(fetch.mock.calls[1][1]).toEqual({
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        removeLabels: [
          {
            assetId: ASSET_ID,
            label: 'Space',
          },
        ],
      }),
    })
  })
})
