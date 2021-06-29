import TestRenderer, { act } from 'react-test-renderer'

import labelToolInfo from '../__mocks__/label_tool_info'
import getLabels from '../__mocks__/get_labels'

import AssetLabelingLabel from '../Label'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

describe('AssetLabelingLabel', () => {
  it('should render properly with no label', async () => {
    require('swr').__setMockUseSWRResponse({ data: getLabels })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const { label, scope } = labelToolInfo.results[0]

    const mockDispatch = jest.fn()
    const mockDelete = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingLabel
        projectId={PROJECT_ID}
        datasetId={DATASET_ID}
        state={{
          label: '',
          scope: 'TRAIN',
          error: '',
        }}
        dispatch={mockDispatch}
        label={labelToolInfo.results[0]}
        onDelete={mockDelete}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Select Label
    act(() => {
      component.root
        .findByProps({ label: 'Label:' })
        .props.onChange({ value: 'cat' })
    })

    expect(mockDispatch).toHaveBeenLastCalledWith({
      label: 'cat',
      scope,
    })

    // Select Scope
    act(() => {
      component.root
        .findByProps({ legend: 'Select Scope' })
        .props.onClick({ value: 'TEST' })
    })

    expect(mockDispatch).toHaveBeenLastCalledWith({
      label,
      scope: 'TEST',
    })
  })

  it('should render properly with a label', async () => {
    require('swr').__setMockUseSWRResponse({ data: getLabels })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const { bbox, label, scope } = labelToolInfo.results[1]

    const mockDispatch = jest.fn()
    const mockDelete = jest.fn()

    const component = TestRenderer.create(
      <AssetLabelingLabel
        projectId={PROJECT_ID}
        datasetId={DATASET_ID}
        state={{
          label: '',
          scope: 'TRAIN',
          error: '',
        }}
        dispatch={mockDispatch}
        label={labelToolInfo.results[1]}
        onDelete={mockDelete}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Delete
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Remove Label' })
        .props.onClick()
    })

    expect(mockDelete).toHaveBeenLastCalledWith({
      label: { bbox, label, scope },
    })
  })
})
