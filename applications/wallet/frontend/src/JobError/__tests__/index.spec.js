import TestRenderer from 'react-test-renderer'

import JobError from '..'
import { jobErrorFatal, jobErrorNonFatal } from '../__mocks__/jobError'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = jobErrorFatal.jobId
const FATAL_ERROR_ID = jobErrorFatal.id
const NON_FATAL_ERROR_ID = jobErrorNonFatal.id

describe('<JobError />', () => {
  it('should render properly when loading', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, errorId: FATAL_ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(<JobError />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a fatal error', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, errorId: FATAL_ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrorFatal,
    })

    const component = TestRenderer.create(<JobError />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a non-fatal error', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: {
        projectId: PROJECT_ID,
        jobId: JOB_ID,
        errorId: NON_FATAL_ERROR_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrorNonFatal,
    })

    const component = TestRenderer.create(<JobError />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
