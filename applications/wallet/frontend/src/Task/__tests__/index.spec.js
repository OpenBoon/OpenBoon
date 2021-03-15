import TestRenderer from 'react-test-renderer'

import task from '../__mocks__/task'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Task from '..'

jest.mock('../../TaskScript', () => 'TaskScript')
jest.mock('../../TaskAssets', () => 'TaskAssets')
jest.mock('../../TaskErrors', () => 'TaskErrors')
jest.mock('../../TaskLogs', () => 'TaskLogs')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '82d5308b-67c2-1433-8fef-0a580a000956'

describe('<Task />', () => {
  it('should render properly for Logs', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/tasks/[taskId]/logs',
      query: {
        projectId: PROJECT_ID,
        jobId: JOB_ID,
        taskId: task.id,
        action: 'Retrying task.',
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...task, timeStarted: -1, state: 'Waiting' },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Task />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for Script', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/tasks/[taskId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, taskId: task.id },
    })

    require('swr').__setMockUseSWRResponse({
      data: task,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Task />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for Assets', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/tasks/[taskId]/assets',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, taskId: task.id },
    })

    require('swr').__setMockUseSWRResponse({
      data: task,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Task />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for Errors', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, taskId: task.id },
    })

    require('swr').__setMockUseSWRResponse({
      data: task,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Task />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
