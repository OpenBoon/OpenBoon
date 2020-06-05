import TestRenderer from 'react-test-renderer'

import jobTasks from '../__mocks__/jobTasks'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import JobTasks from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = 'c097596f-62ef-1f81-83f8-0a580a000954'

jest.mock('../../Date/helpers', () => ({
  ...jest.requireActual('../../Date/helpers'),
  getDuration: () => 1587969769607,
}))

describe('<JobTasks />', () => {
  it('should render properly with job tasks', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/tasks',
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobTasks,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <JobTasks />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly without job tasks', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/tasks',
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {},
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <JobTasks />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
