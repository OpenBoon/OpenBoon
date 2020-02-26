import TestRenderer from 'react-test-renderer'

import JobErrorType from '..'

import { jobErrorFatal, jobErrorNonFatal } from '../__mocks__/jobError'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '223fd17d-7028-1519-94a8-d2f0132bc0c8'
const ERROR_ID = jobErrorNonFatal.id
const FATAL_ERROR_ID = jobErrorFatal.id

describe('<JobErrorType />', () => {
  it('should render properly when fetching errors', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, errorId: ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: jobErrorNonFatal })

    const { message, fatal } = jobErrorFatal
    const component = TestRenderer.create(
      <JobErrorType error={{ message, fatal }} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, errorId: FATAL_ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrorFatal,
    })

    const { message, fatal } = jobErrorNonFatal
    const component = TestRenderer.create(
      <JobErrorType error={{ message, fatal }} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
