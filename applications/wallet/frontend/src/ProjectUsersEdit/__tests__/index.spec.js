import TestRenderer, { act } from 'react-test-renderer'

import projectUser from '../../ProjectUser/__mocks__/projectUser'
import roles from '../../Roles/__mocks__/roles'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import ProjectUsersEdit from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const USER_ID = projectUser.id

const noop = () => () => {}

describe('<ProjectUsersEdit />', () => {
  it('should render properly with a user and roles', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/users/[userId]/edit',
      query: { projectId: PROJECT_ID, userId: USER_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        ...projectUser,
        ...roles,
      },
    })

    // Render Form
    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ProjectUsersEdit />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Click checkbox
    await act(async () => {
      component.root
        .findByProps({ value: 'ML_Tools' })
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
      body: '{"roles":["API_Keys"]}',
    })

    expect(mockFn).toHaveBeenCalledWith(
      '/[projectId]/users?action=edit-user-success',
      `/${PROJECT_ID}/users`,
    )
  })
})
