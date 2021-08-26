import TestRenderer, { act } from 'react-test-renderer'

import getLabels from '../../AssetLabeling/__mocks__/get_labels'

import BulkAssetLabelingForm from '../Form'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

describe('BulkAssetLabelingForm', () => {
  it('should render properly', async () => {
    require('swr').__setMockUseSWRResponse({ data: getLabels })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <BulkAssetLabelingForm
        projectId={PROJECT_ID}
        datasetType="Classification"
        state={{
          datasetId: DATASET_ID,
          lastLabel: '',
          lastScope: 'TRAIN',
          errors: {},
        }}
        dispatch={mockDispatch}
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
      lastLabel: 'cat',
      lastScope: 'TRAIN',
    })

    // Select Scope
    act(() => {
      component.root
        .findByProps({ legend: 'Select Scope' })
        .props.onClick({ value: 'TEST' })
    })

    expect(mockDispatch).toHaveBeenLastCalledWith({
      lastLabel: '',
      lastScope: 'TEST',
    })
  })

  it('should render properly for a non-Classification dataset', async () => {
    require('swr').__setMockUseSWRResponse({ data: getLabels })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/visualizer',
      query: { projectId: PROJECT_ID },
    })

    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <BulkAssetLabelingForm
        projectId={PROJECT_ID}
        datasetType="FaceRecognition"
        state={{
          datasetId: DATASET_ID,
          lastLabel: '',
          lastScope: 'TRAIN',
          errors: {},
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
