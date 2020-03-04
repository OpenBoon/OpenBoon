import TestRenderer, { act } from 'react-test-renderer'

import projectUser from '../../ProjectUser/__mocks__/projectUser'
import permissions from '../../Permissions/__mocks__/permissions'

import ProjectUsersEdit from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const USER_ID = projectUser.id

const noop = () => () => {}

describe('<ProjectUsersEdit />', () => {
  it('should render properly with a user and permissions', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/users/[userId]/edit',
      query: { projectId: PROJECT_ID, userId: USER_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        ...projectUser,
        ...permissions,
      },
    })

    // Render Form
    const component = TestRenderer.create(<ProjectUsersEdit />)

    expect(component.toJSON()).toMatchSnapshot()

    // Click checkbox
    await act(async () => {
      component.root
        .findByProps({ value: 'AssetsRead' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    // Dismiss Error Message
    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Close alert' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify(projectUser))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(2)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/users/${USER_ID}/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: '{"permissions":["ApiKeyManage","ProjectManage","AssetsRead"]}',
    })

    expect(mockFn).toHaveBeenCalledWith(
      '/[projectId]/users?action=edit-user-success',
      `/${PROJECT_ID}/users?action=edit-user-success`,
    )
  })
})
