import TestRenderer, { act } from 'react-test-renderer'

import triggers from '../../Triggers/__mocks__/triggers'
import webhooks from '../../Webhooks/__mocks__/webhooks'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import WebhooksEdit from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

const noop = () => () => {}

describe('<WebhooksEdit />', () => {
  it('should render properly', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/webhooks/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...triggers, ...webhooks.results[0] },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <WebhooksEdit />
      </User>,
    )

    // Input URL
    act(() => {
      component.root
        .findByProps({ id: 'url' })
        .props.onChange({ target: { value: '127.0.0.1' } })
    })

    // Generate Token
    act(() => {
      component.root
        .findByProps({ children: 'Generate Token' })
        .props.onClick({ preventDefault: noop })
    })

    act(() => {
      component.root
        .findByProps({ id: 'secretKey' })
        .props.onChange({ target: { value: 'super-secret-token' } })
    })

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Show Token' })
        .props.onClick({ preventDefault: noop })
    })

    // Edit Trigger
    act(() => {
      component.root
        .findByProps({ value: 'ASSET_ANALYZED' })
        .props.onClick({ preventDefault: noop })
    })

    // Activate
    act(() => {
      component.root
        .findByProps({ value: 'active' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Failure
    fetch.mockResponseOnce(
      JSON.stringify({ detail: ['Something went wrong.'] }),
      {
        status: 500,
      },
    )

    // Send Test
    await act(async () => {
      component.root
        .findAllByProps({ children: 'Send Test' })[0]
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({}), {
      headers: { 'content-type': 'application/json' },
    })

    // Send Test Again
    await act(async () => {
      component.root
        .findAllByProps({ children: 'Send Test' })[0]
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Submit Failure
    fetch.mockResponseOnce(JSON.stringify({ url: ['URL already in use'] }), {
      status: 400,
    })

    // Submit form
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Submit Success
    fetch.mockResponseOnce(JSON.stringify({}), {
      headers: { 'content-type': 'application/json' },
    })

    // Submit form
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/webhooks?action=edit-webhook-success',
      `/${PROJECT_ID}/webhooks`,
    )
  })
})
