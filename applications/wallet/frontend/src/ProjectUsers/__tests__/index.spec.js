import TestRenderer from 'react-test-renderer'

import projectUsers from '../__mocks__/projectUsers'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import ProjectUsers from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ProjectUsers />', () => {
  it('should render properly with no project users', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/users',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ProjectUsers />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with project users', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/users',
      query: { projectId: PROJECT_ID, action: 'edit-user-success' },
    })

    require('swr').__setMockUseSWRResponse({
      data: projectUsers,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ProjectUsers />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should hide the menu gear for the active user', () => {
    require('swr').__setMockUseSWRResponse({
      data: projectUsers,
    })

    const component = TestRenderer.create(
      <User initialUser={{ ...mockUser, email: 'jane@zorroa.com' }}>
        <ProjectUsers />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
