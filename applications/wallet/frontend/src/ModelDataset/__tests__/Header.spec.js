import TestRenderer, { act } from 'react-test-renderer'

import model from '../../Model/__mocks__/model'
import dataset from '../../Dataset/__mocks__/dataset'

import ModelDatasetHeader from '../Header'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

const noop = () => () => {}

describe('<ModelDatasetHeader />', () => {
  it('should render properly', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: dataset })

    const component = TestRenderer.create(
      <ModelDatasetHeader
        projectId={PROJECT_ID}
        modelId={MODEL_ID}
        model={{ ...model, datasetId: DATASET_ID }}
        setErrors={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Failure
    fetch.mockResponseOnce(JSON.stringify({ name: ['Name already in use'] }), {
      status: 400,
    })

    // Unlink
    act(() => {
      component.root.findByProps({ children: 'Unlink Dataset' }).props.onClick()
    })

    // Cancel
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    // Unlink
    act(() => {
      component.root.findByProps({ children: 'Unlink Dataset' }).props.onClick()
    })

    await act(async () => {
      component.root.findByProps({ children: 'Unlink' }).props.onClick()
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({}))

    // Unlink
    act(() => {
      component.root.findByProps({ children: 'Unlink Dataset' }).props.onClick()
    })

    await act(async () => {
      component.root.findByProps({ children: 'Unlink' }).props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(2)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        datasetId: null,
        name: model.name,
      }),
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/models/[modelId]?action=unlink-dataset-success',
      `/${PROJECT_ID}/models/${MODEL_ID}`,
    )
  })
})
