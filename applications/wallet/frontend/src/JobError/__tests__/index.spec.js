import TestRenderer from 'react-test-renderer'

import JobError from '..'
import { jobErrorFatal } from '../__mocks__/jobError'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = jobErrorFatal.jobId
const ERROR_ID = jobErrorFatal.id

describe('<JobError />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, errorId: ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrorFatal,
    })

    const component = TestRenderer.create(<JobError />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
