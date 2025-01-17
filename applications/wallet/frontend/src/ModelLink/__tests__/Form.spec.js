import TestRenderer, { act } from 'react-test-renderer'

import model from '../../Model/__mocks__/model'
import datasets from '../../Datasets/__mocks__/datasets'
import datasetTypes from '../../DatasetTypes/__mocks__/datasetTypes'

import ModelLinkForm from '../Form'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = model.id
const DATASET_ID = datasets.results[0].id

describe('<ModelLinkForm />', () => {
  it('should render properly for Existing', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/link',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: datasets })

    const component = TestRenderer.create(
      <ModelLinkForm model={{ ...model, datasetType: 'FaceRecognition' }} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Select source
    act(() => {
      component.root
        .findByProps({ value: 'EXISTING' })
        .props.onClick({ value: 'EXISTING' })
    })

    // Select dataset
    act(() => {
      component.root
        .findByProps({ label: 'Dataset' })
        .props.onChange({ value: datasets.results[0].id })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Link Dataset' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ id: MODEL_ID }), {
      headers: { 'content-type': 'application/json' },
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Link Dataset' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(4)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/${MODEL_ID}/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        datasetId: datasets.results[0].id,
      }),
    })

    expect(mockFn).toHaveBeenCalledWith(
      `/[projectId]/models/[modelId]?action=link-dataset-success`,
      `/${PROJECT_ID}/models/${MODEL_ID}`,
    )
  })

  it('should render properly for New', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/link',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        results: datasetTypes.results.map((datasetType) => ({
          id: datasetType.name,
          ...datasetType,
        })),
      },
    })

    const component = TestRenderer.create(
      <ModelLinkForm model={{ ...model, datasetType: 'Classification' }} />,
    )

    // Select source
    act(() => {
      component.root
        .findByProps({ value: 'NEW' })
        .props.onClick({ value: 'NEW' })
    })

    // Input valid name
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'My New Dataset' } })
    })

    // Input valid description
    act(() => {
      component.root
        .findByProps({ id: 'description' })
        .props.onChange({ target: { value: 'Lorem Ipsum' } })
    })

    // Select valid type
    act(() => {
      component.root
        .findByProps({ value: 'Classification' })
        .props.onClick({ value: 'Classification' })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Failure
    fetch.mockResponseOnce(JSON.stringify({ name: ['Name already in use'] }), {
      status: 400,
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Link Dataset' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Link Dataset' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ id: DATASET_ID }), {
      headers: { 'content-type': 'application/json' },
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Link Dataset' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(7)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/datasets/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        name: 'My New Dataset',
        description: 'Lorem Ipsum',
        type: 'Classification',
      }),
    })

    expect(mockFn).toHaveBeenCalledWith(
      `/[projectId]/models/[modelId]?action=link-dataset-success`,
      `/${PROJECT_ID}/models/${MODEL_ID}`,
    )
  })
})
