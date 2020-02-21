import TestRenderer from 'react-test-renderer'

import TaskError from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '223fd17d-7028-1519-94a8-d2f0132bc0c8'
const TASK_ID = 'b81f47e9-7382-1519-b88a-d2f0132bc0c8'

describe('<TaskError />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[taskId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, taskId: TASK_ID },
    })

    const component = TestRenderer.create(<TaskError />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
