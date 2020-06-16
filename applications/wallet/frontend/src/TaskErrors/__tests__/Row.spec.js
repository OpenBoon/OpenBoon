import TestRenderer, { act } from 'react-test-renderer'

import TaskErrorsRow from '../Row'

import taskErrors from '../__mocks__/taskErrors'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = 'c097596f-62ef-1f81-83f8-0a580a000954'
const ERROR = taskErrors.results[0]
const TASK_ID = ERROR.taskId

const noop = () => () => {}

describe('<TaskErrorsRow />', () => {
  it('should navigate on a click on the row directly', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <TaskErrorsRow
        projectId={PROJECT_ID}
        jobId={JOB_ID}
        error={ERROR}
        revalidate={noop}
      />,
    )

    act(() => {
      component.root.findByType('tr').props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]',
      `/${PROJECT_ID}/jobs/${JOB_ID}/tasks/${TASK_ID}/errors/${ERROR.id}`,
    )
  })

  it('should not navigate on a click on a link', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <TaskErrorsRow
        projectId={PROJECT_ID}
        jobId={JOB_ID}
        error={ERROR}
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
