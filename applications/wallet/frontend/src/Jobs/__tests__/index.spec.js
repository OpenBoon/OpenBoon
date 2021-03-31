import TestRenderer from 'react-test-renderer'

import jobs from '../__mocks__/jobs'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Jobs from '..'

jest.mock('../../Pagination', () => 'Pagination')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Jobs />', () => {
  it('should render properly with no jobs', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID, sort: 'timeCreated' },
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
        <Jobs />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with jobs', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID, sort: '-timeCreated' },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobs,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Jobs />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
