import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'
import models from '../../Models/__mocks__/models'
import project from '../../Project/__mocks__/project'

import AssetLabeling from '..'

const PROJECT_ID = project.id
const ASSET_ID = asset.id
const MODEL_ID = models.results[0].id
const ALT_MODEL_ID = models.results[2].id
const EXISTING_ASSET_LABEL = asset.metadata.labels[0].label

jest.mock('../../Combobox', () => 'Combobox')

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

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Combobox').props.options()
    })

    // Click Accordion header link
    act(() => {
      component.root
        .findByProps({ children: 'Create New Model' })
        .props.onClick({ stopPropagation: noop })
    })

    // Select Model
    act(() => {
      component.root
        .findByProps({ label: 'Model' })
        .props.onChange({ value: MODEL_ID })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Input Label
    act(() => {
      component.root
        .findByType('Combobox')
        .props.onChange({ value: 'Flimflarm' })
    })

    expect(component.toJSON()).toMatchSnapshot()

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

    // Select Model
    act(() => {
      component.root
        .findByProps({ label: 'Model' })
        .props.onChange({ value: ALT_MODEL_ID })
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

    // Edit Model/Label fields
    act(() => {
      component.root
        .findByProps({ label: 'Model' })
        .props.onChange({ value: ALT_MODEL_ID })
      component.root
        .findByType('Combobox')
        .props.onChange({ value: 'FlamFlirm' })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Clear Model/Label fields
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Open Asset Label list Accordion
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Expand Section' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

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

    expect(component.toJSON()).toMatchSnapshot()

    expect(fetch.mock.calls.length).toEqual(7)

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
        remove_labels: [
          {
            assetId: ASSET_ID,
            label: EXISTING_ASSET_LABEL,
          },
        ],
        add_labels: [
          {
            assetId: ASSET_ID,
            label: 'Flimflarm',
          },
        ],
      }),
    })

    // Call from submitting label for un-used model
    expect(fetch.mock.calls[3][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${ALT_MODEL_ID}/add_labels/`,
    )

    expect(fetch.mock.calls[3][1]).toEqual({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        add_labels: [
          {
            assetId: ASSET_ID,
            label: 'Other Flimflarm',
          },
        ],
      }),
    })

    // Call from submitting empty label value
    expect(fetch.mock.calls[5][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/delete_labels/`,
    )

    expect(fetch.mock.calls[5][1]).toEqual({
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        remove_labels: [
          {
            assetId: ASSET_ID,
            label: 'Space',
          },
        ],
      }),
    })

    // Call from Delete Modal
    expect(fetch.mock.calls[6][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/delete_labels/`,
    )

    expect(fetch.mock.calls[6][1]).toEqual({
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        remove_labels: [
          {
            assetId: ASSET_ID,
            label: 'Space',
          },
        ],
      }),
    })
  })

  it('should render properly with no labels', () => {
    require('swr').__setMockUseSWRResponse({
      data: { ...models, metadata: { source: asset.metadata.source } },
    })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(<AssetLabeling />)

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Expand Section' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
