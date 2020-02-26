import TestRenderer from 'react-test-renderer'

import JobErrorType from '..'

import { jobErrorFatal, jobErrorNonFatal } from '../__mocks__/jobError'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ERROR_ID = jobErrorNonFatal.id
const FATAL_ERROR_ID = jobErrorFatal.id

describe('<JobErrorType />', () => {
  it('should render properly when when error is non-fatal', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, errorId: ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: jobErrorNonFatal })

    const component = TestRenderer.create(
      <JobErrorType error={jobErrorFatal} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when error is fatal', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, errorId: FATAL_ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrorFatal,
    })

    const component = TestRenderer.create(
      <JobErrorType error={jobErrorFatal} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
