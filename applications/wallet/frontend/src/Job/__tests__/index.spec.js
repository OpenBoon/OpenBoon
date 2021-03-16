import TestRenderer from 'react-test-renderer'

import job from '../__mocks__/job'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Job from '..'

jest.mock('../../Breadcrumbs', () => 'Breadcrumbs')
jest.mock('../../JobTasks', () => 'JobTasks')
jest.mock('../../TaskErrors', () => 'TaskErrors')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '82d5308b-67c2-1433-8fef-0a580a000956'

describe('<Job />', () => {
  it('should render properly for Tasks', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]',
      query: {
        projectId: PROJECT_ID,
        jobId: JOB_ID,
        action: 'Retry All Failures',
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: job,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Job />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for Errors', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors',
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...job, paused: true },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Job />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
