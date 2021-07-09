import TestRenderer, { act } from 'react-test-renderer'

import modelTypes from '../../ModelTypes/__mocks__/modelTypes'
import model from '../__mocks__/model'

import ModelDetails from '../Details'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'
const JOB_ID = '223fd17d-7028-1519-94a8-d2f0132bc0c8'

jest.mock('next/link', () => 'Link')
jest.mock('../MatrixLink', () => 'ModelMatrixLink')

const noop = () => () => {}

describe('<ModelDetails />', () => {
  it('should render properly when trained and applied', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        ...model,
        timeLastTrained: 1625774562852,
        timeLastApplied: 1625774664673,
        modelTypeRestrictions: { missingLabels: 0 },
      },
    })

    const component = TestRenderer.create(
      <ModelDetails
        projectId={PROJECT_ID}
        modelId={MODEL_ID}
        modelTypes={modelTypes.results}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly', async () => {
    const mockMutate = jest.fn()

    require('swr').__setMockMutateFn(mockMutate)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...model, modelTypeRestrictions: { missingLabels: 0 } },
    })

    const component = TestRenderer.create(
      <ModelDetails
        projectId={PROJECT_ID}
        modelId={MODEL_ID}
        modelTypes={modelTypes.results}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    // Select Delete
    act(() => {
      component.root.findByProps({ children: 'Delete Model' }).props.onClick()
    })

    // Cancel
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    // Mock Failure
    fetch.mockResponseOnce(null, { status: 500 })

    await act(async () => {
      component.root
        .findByProps({ children: 'Train Model', isDisabled: false })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    // Mock Failure
    fetch.mockResponseOnce(null, { status: 400 })

    await act(async () => {
      component.root
        .findByProps({ children: 'Train & Test', isDisabled: false })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ jobId: JOB_ID }), {
      headers: { 'content-type': 'application/json' },
    })

    await act(async () => {
      component.root
        .findByProps({ children: 'Train & Analyze All', isDisabled: false })
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
      body: '{"apply":false,"test":false}',
    })

    expect(mockMutate).toHaveBeenCalledWith({
      ...model,
      modelTypeRestrictions: { missingLabels: 0 },
      runningJobId: JOB_ID,
      ready: true,
    })
  })
})
