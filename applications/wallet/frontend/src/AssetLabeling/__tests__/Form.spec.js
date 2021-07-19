import TestRenderer, { act } from 'react-test-renderer'

import labelToolInfo from '../__mocks__/label_tool_info'

import AssetLabelingForm from '../Form'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

jest.mock('../Label', () => 'AssetLabelingLabel')

describe('AssetLabelingForm', () => {
  it('should render properly when saving', async () => {
    require('swr').__setMockUseSWRResponse({ data: labelToolInfo })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingForm
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        hasFaceDetection
        state={{
          datasetId: DATASET_ID,
          datasetType: 'Classification',
          isLoading: true,
          labels: {},
          errors: {},
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Cancel
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    expect(mockDispatch).toHaveBeenLastCalledWith({ datasetId: '', labels: {} })

    // Label Delete
    act(() => {
      component.root
        .findAllByProps({ projectId: PROJECT_ID, datasetId: DATASET_ID })[1]
        .props.onDelete({ label: 'cat' })
    })

    expect(mockDispatch).toHaveBeenLastCalledWith({
      errors: {},
      isLoading: true,
    })
  })

  it('should not try to save when there are no changes', async () => {
    require('swr').__setMockUseSWRResponse({ data: labelToolInfo })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingForm
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        hasFaceDetection
        state={{
          datasetId: DATASET_ID,
          datasetType: 'Classification',
          isLoading: false,
          labels: {},
          errors: {},
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Save
    act(() => {
      component.root.findByProps({ children: 'Save' }).props.onClick()
    })

    expect(mockDispatch).not.toHaveBeenCalled()
  })

  it('should save properly when there are Classification changes', async () => {
    require('swr').__setMockUseSWRResponse({ data: labelToolInfo })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingForm
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        hasFaceDetection
        state={{
          datasetId: DATASET_ID,
          datasetType: 'Classification',
          isLoading: false,
          labels: {
            [ASSET_ID]: {
              scope: 'TRAIN',
              label: 'pony',
            },
          },
          errors: {},
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Label Dispatch
    act(() => {
      component.root
        .findAllByProps({ projectId: PROJECT_ID, datasetId: DATASET_ID })[2]
        .props.dispatch({ label: 'cat', scope: 'TEST' })
    })

    expect(mockDispatch).toHaveBeenLastCalledWith({
      labels: {
        [ASSET_ID]: {
          label: 'cat',
          scope: 'TEST',
        },
      },
      lastLabel: 'cat',
      lastScope: 'TEST',
    })

    // Save
    act(() => {
      component.root.findByProps({ children: 'Save' }).props.onClick()
    })

    expect(mockDispatch).toHaveBeenLastCalledWith({
      errors: {},
      isLoading: true,
    })
  })

  it('should save properly when there are FaceRecognition changes', async () => {
    require('swr').__setMockUseSWRResponse({ data: labelToolInfo })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingForm
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        hasFaceDetection
        state={{
          datasetId: DATASET_ID,
          datasetType: 'FaceRecognition',
          isLoading: false,
          labels: {
            '[0.53,0.113,0.639,0.29]': {
              scope: 'TRAIN',
              label: 'pony',
            },
          },
          errors: {},
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Label Dispatch
    act(() => {
      component.root
        .findAllByProps({ projectId: PROJECT_ID, datasetId: DATASET_ID })[0]
        .props.dispatch({ label: 'cat', scope: 'TEST' })
    })

    expect(mockDispatch).toHaveBeenLastCalledWith({
      labels: {
        '[0.53,0.113,0.639,0.29]': {
          label: 'cat',
          scope: 'TEST',
        },
      },
      lastLabel: 'cat',
      lastScope: 'TEST',
    })

    // Save
    act(() => {
      component.root.findByProps({ children: 'Save' }).props.onClick()
    })

    expect(mockDispatch).toHaveBeenLastCalledWith({
      errors: {},
      isLoading: true,
    })
  })

  it("should run face detection when it's missing", async () => {
    require('swr').__setMockUseSWRResponse({ data: { count: 0, results: [] } })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingForm
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        hasFaceDetection={false}
        state={{
          datasetId: DATASET_ID,
          datasetType: 'FaceRecognition',
          isLoading: false,
          labels: {},
          errors: {},
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Run Face Detection
    act(() => {
      component.root
        .findByProps({ children: 'Run Face Detection On This Asset' })
        .props.onClick()
    })

    expect(mockDispatch).toHaveBeenLastCalledWith({
      errors: {},
      isLoading: true,
    })
  })

  it('should render that face detection is running', async () => {
    require('swr').__setMockUseSWRResponse({ data: { count: 0, results: [] } })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingForm
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        hasFaceDetection={false}
        state={{
          datasetId: DATASET_ID,
          datasetType: 'FaceRecognition',
          isLoading: true,
          labels: {},
          errors: {},
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when there are no faces detected', async () => {
    require('swr').__setMockUseSWRResponse({ data: { count: 0, results: [] } })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingForm
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        hasFaceDetection
        state={{
          datasetId: DATASET_ID,
          datasetType: 'FaceRecognition',
          isLoading: false,
          labels: {},
          errors: {},
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
