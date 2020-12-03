import TestRenderer, { act } from 'react-test-renderer'

import model from '../__mocks__/model'

import ModelDetails from '../Details'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'
const JOB_ID = '223fd17d-7028-1519-94a8-d2f0132bc0c8'

const noop = () => () => {}

jest.mock('next/link', () => 'Link')
jest.mock('../../ModelAssets', () => 'ModelAssets')

describe('<ModelDetails />', () => {
  it('should handle train errors properly', async () => {
    const mockMutate = jest.fn()

    require('swr').__setMockMutateFn(mockMutate)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        ...model,
        runningJobId: '',
        modelTypeRestrictions: {
          requiredLabels: 2,
          missingLabels: 0,
          requiredAssetsPerLabel: 10,
          missingLabelsOnAssets: 0,
        },
      },
    })

    const component = TestRenderer.create(<ModelDetails />)

    // Mock Failure
    fetch.mockResponseOnce(null, { status: 500 })

    await act(async () => {
      component.root
        .findByProps({ children: 'Train' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/train/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'POST',
      body: '{"deploy":false}',
    })
  })

  it('should handle train & apply success properly', async () => {
    const mockMutate = jest.fn()

    require('swr').__setMockMutateFn(mockMutate)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...model, runningJobId: '' },
    })

    const component = TestRenderer.create(<ModelDetails />)

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ jobId: JOB_ID }), {
      headers: { 'content-type': 'application/json' },
    })

    await act(async () => {
      component.root
        .findByProps({ children: 'Train & Apply' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/train/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'POST',
      body: '{"deploy":true}',
    })

    expect(mockMutate).toHaveBeenCalledWith({
      ...model,
      runningJobId: JOB_ID,
      ready: true,
    })
  })

  it('should render properly with less than required labels for training', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        ...model,
        runningJobId: '',
        modelTypeRestrictions: {
          requiredLabels: 2,
          missingLabels: 1,
          requiredAssetsPerLabel: 10,
          missingLabelsOnAssets: 1,
        },
      },
    })

    const component = TestRenderer.create(<ModelDetails />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with less than required labels for training', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        ...model,
        runningJobId: '',
        modelTypeRestrictions: {
          requiredLabels: 2,
          missingLabels: 2,
          requiredAssetsPerLabel: 10,
          missingLabelsOnAssets: 1,
        },
      },
    })

    const component = TestRenderer.create(<ModelDetails />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render Labeled Assets properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/assets',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        ...model,
        runningJobId: '',
        modelTypeRestrictions: {
          requiredLabels: 2,
          missingLabels: 2,
          requiredAssetsPerLabel: 10,
          missingLabelsOnAssets: 1,
        },
      },
    })

    const component = TestRenderer.create(<ModelDetails />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should handle filter properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...model, runningJobId: '' },
    })

    const component = TestRenderer.create(<ModelDetails />)

    // eslint-disable-next-line no-proto
    const spy = jest.spyOn(localStorage.__proto__, 'setItem')

    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Add Filter in Visualizer' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(spy).toHaveBeenCalledWith('rightOpeningPanel', '"filters"')
  })

  it('should handle Add More Labels properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        ...model,
        runningJobId: '',
      },
    })

    const component = TestRenderer.create(<ModelDetails />)

    // eslint-disable-next-line no-proto
    const spy = jest.spyOn(localStorage.__proto__, 'setItem')

    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Add More Labels' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(spy).toHaveBeenCalledWith('leftOpeningPanel', '"assetLabeling"')

    expect(spy).toHaveBeenCalledWith(
      `AssetLabelingAdd.${PROJECT_ID}`,
      `{"modelId":"${MODEL_ID}","label":"","scope":""}`,
    )
  })

  it('should handle delete properly', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    fetch.mockResponseOnce('{}')

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...model, runningJobId: '' },
    })

    const component = TestRenderer.create(<ModelDetails />)

    // Open Delete Modal
    act(() => {
      component.root
        .findByProps({ children: 'Delete' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    // Cancel Delete Modal
    act(() => {
      component.root
        .findByProps({ children: 'Cancel' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    // Open Delete Modal
    act(() => {
      component.root
        .findByProps({ children: 'Delete' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    // Confirm
    await act(async () => {
      component.root
        .findByProps({ children: 'Delete Permanently' })
        .props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(3)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'DELETE',
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/models?action=delete-model-success',
      `/${PROJECT_ID}/models`,
    )
  })
})
