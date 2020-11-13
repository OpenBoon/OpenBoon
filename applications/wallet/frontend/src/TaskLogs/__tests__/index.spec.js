import TestRenderer from 'react-test-renderer'

import taskLogs from '../__mocks__/taskLogs'

import TaskLogs from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '82d5308b-67c2-1433-8fef-0a580a000956'
const TASK_ID = '5262c1ef-91ad-1d33-82b6-d6edb1b855c4'

describe('<TaskLogs />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/tasks/[taskId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, taskId: TASK_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: taskLogs })

    const component = TestRenderer.create(<TaskLogs />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
