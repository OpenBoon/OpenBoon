import TestRenderer, { act } from 'react-test-renderer'

import model from '../../Model/__mocks__/model'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import ModelsEdit from '..'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = model.id

describe('<ModelsEdit />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/edit',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ModelsEdit projectId={PROJECT_ID} model={model} />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Input valid name
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'My New Model' } })
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
        .findByProps({ type: 'submit', children: 'Save Model' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Save Model' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ id: MODEL_ID }), {
      headers: { 'content-type': 'application/json' },
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Save Model' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(5)

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
        name: 'My New Model',
        description: 'Lorem Ipsum',
      }),
    })

    expect(mockFn).toHaveBeenCalledWith(
      `/[projectId]/models/[modelId]?action=edit-model-success`,
      `/${PROJECT_ID}/models/${MODEL_ID}`,
    )
  })
})
