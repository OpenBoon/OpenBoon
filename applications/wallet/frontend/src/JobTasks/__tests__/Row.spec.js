import TestRenderer, { act } from 'react-test-renderer'

import JobTasksRow from '../Row'

import jobTasks from '../__mocks__/jobTasks'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '82d5308b-67c2-1433-8fef-0a580a000956'
const TASK = jobTasks.results[0]

const noop = () => () => {}

describe('<JobTasksRow />', () => {
  it('should navigate on a click on the row directly', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <JobTasksRow
        projectId={PROJECT_ID}
        jobId={JOB_ID}
        task={TASK}
        revalidate={noop}
      />,
    )

    act(() => {
      component.root.findByType('tr').props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/jobs/[jobId]/tasks/[taskId]/script',
      `/${PROJECT_ID}/jobs/${JOB_ID}/tasks/${TASK.id}/script`,
    )
  })

  it('should not navigate on a click on a link', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <JobTasksRow
        projectId={PROJECT_ID}
        jobId={JOB_ID}
        task={TASK}
        revalidate={noop}
      />,
    )

    act(() => {
      component.root
        .findByType('tr')
        .props.onClick({ target: { localName: 'a' } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()
  })
})
