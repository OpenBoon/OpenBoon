import TestRenderer, { act } from 'react-test-renderer'

import apiKey from '../../ApiKey/__mocks__/apiKey'
import permissions from '../../Permissions/__mocks__/permissions'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import ApiKeysAdd from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

const noop = () => () => {}

describe('<ApiKeysAdd />', () => {
  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: permissions,
    })

    const mockCopyFn = jest.fn()

    window.navigator.clipboard.writeText = mockCopyFn

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ApiKeysAdd />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Input API Key name
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'API Key Name' } })
    })

    // Check permission box
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'ProjectManage' })
        .props.onClick()
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify(apiKey), {
      headers: { 'content-type': 'application/json' },
    })

    // Submit form
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockCopyFn).toHaveBeenCalledWith(JSON.stringify(apiKey))

    // Reset form
    act(() => {
      component.root
        .findByProps({ children: 'Create Another Key' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
