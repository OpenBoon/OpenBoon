import TestRenderer, { act } from 'react-test-renderer'

import datasets from '../../Datasets/__mocks__/datasets'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import DatasetsEdit from '..'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = datasets.results[0].id

describe('<DatasetsEdit />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/datasets/[datasetId]/edit',
      query: { projectId: PROJECT_ID, datasetId: DATASET_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: datasets.results[0] })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DatasetsEdit />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

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

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Failure
    fetch.mockResponseOnce(JSON.stringify({ name: ['Name already in use'] }), {
      status: 400,
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Save Dataset' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Save Dataset' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ id: DATASET_ID }), {
      headers: { 'content-type': 'application/json' },
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Save Dataset' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(6)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/datasets/${DATASET_ID}/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        id: DATASET_ID,
        name: 'My New Dataset',
        description: 'Lorem Ipsum',
        type: 'FaceRecognition',
      }),
    })

    expect(mockFn).toHaveBeenCalledWith(
      `/[projectId]/datasets/[datasetId]?action=edit-dataset-success`,
      `/${PROJECT_ID}/datasets/${DATASET_ID}`,
    )
  })
})
