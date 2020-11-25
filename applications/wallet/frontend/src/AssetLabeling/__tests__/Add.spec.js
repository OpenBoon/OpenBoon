import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'
import models from '../../Models/__mocks__/models'

import AssetLabelingAdd from '../Add'

const MODELS = models.results
const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = asset.id
const MODEL_ID = models.results[0].id
const ASSET_LABELS = asset.metadata.labels

jest.mock('../../Combobox', () => 'Combobox')

const noop = () => () => {}

describe('<AssetLabelingAdd />', () => {
  it('should add a label', async () => {
    const component = TestRenderer.create(
      <AssetLabelingAdd
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        models={MODELS}
        labels={[]}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Combobox').props.options()
    })

    // Select Model
    act(() => {
      component.root
        .findByProps({ label: 'Model' })
        .props.onChange({ value: MODEL_ID })
    })

    // Input Label
    act(() => {
      component.root
        .findByType('Combobox')
        .props.onChange({ value: 'Flimflarm' })
    })

    // Select Scope
    act(() => {
      component.root
        .findByProps({ label: 'Scope' })
        .props.onChange({ value: 'TEST' })
    })

    // Mock Failure
    fetch.mockResponseOnce(
      JSON.stringify({ label: ['I guess you cannot use this label'] }),
      {
        status: 400,
      },
    )

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Label Saved' }))

    // Click Submit (updating existing model/label)
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    // Clear Model/Label fields
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(3)

    // Call from submitting label for un-used model
    expect(fetch.mock.calls[2][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/add_labels/`,
    )

    expect(fetch.mock.calls[2][1]).toEqual({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        addLabels: [
          {
            assetId: ASSET_ID,
            label: 'Flimflarm',
            scope: 'TEST',
          },
        ],
      }),
    })
  })

  it('should render with localStorage and update a label', async () => {
    localStorage.setItem(
      `AssetLabelingAdd.${PROJECT_ID}`,
      `{"modelId":"${MODEL_ID},"scope":"TRAIN","label":"Existing localStorage"}`,
    )

    const component = TestRenderer.create(
      <AssetLabelingAdd
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        models={MODELS}
        labels={[
          {
            modelId: MODEL_ID,
            label: 'Existing',
          },
        ]}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Select Model
    act(() => {
      component.root
        .findByProps({ label: 'Model' })
        .props.onChange({ value: MODEL_ID })
    })

    // Input Label
    act(() => {
      component.root
        .findByType('Combobox')
        .props.onChange({ value: 'Other Flimflarm' })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Label Saved' }))

    // Click Submit (adding to new model)
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(1)

    // Call from submitting label for used model
    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/update_labels/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        removeLabels: [
          {
            assetId: ASSET_ID,
            label: 'Existing',
          },
        ],
        addLabels: [
          {
            assetId: ASSET_ID,
            label: 'Other Flimflarm',
            scope: 'TRAIN',
          },
        ],
      }),
    })
  })

  it('should delete a label when passed an empty input', async () => {
    const component = TestRenderer.create(
      <AssetLabelingAdd
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        models={MODELS}
        labels={ASSET_LABELS}
      />,
    )

    // Select Model
    act(() => {
      component.root
        .findByProps({ label: 'Model' })
        .props.onChange({ value: MODEL_ID })
    })

    // Input Label
    act(() => {
      component.root.findByType('Combobox').props.onChange({ value: '' })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Label Deleted' }))

    // Click Submit (with empty label value to delete label)
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(1)

    // Call from submitting empty label value
    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/delete_labels/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
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
