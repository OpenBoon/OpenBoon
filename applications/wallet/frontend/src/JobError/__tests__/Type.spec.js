import TestRenderer from 'react-test-renderer'

import JobErrorType from '..'

import job from '../../Job/__mocks__/job'
import jobErrors from '../../JobErrors/__mocks__/jobErrors'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '223fd17d-7028-1519-94a8-d2f0132bc0c8'
const ERROR_ID = '916c86bc-74b9-1519-b065-d2f0132bc0c8'
const FATAL_ERROR_ID = '916c86bb-74b9-1519-b065-d2f0132bc0c8'

describe('<JobErrorType />', () => {
  it('should render properly when fetching errors', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, errorId: ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        ...job,
        ...jobErrors,
      },
    })

    const nonFatalError = jobErrors.results[0]
    const { message, fatal } = nonFatalError
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
      data: {
        ...job,
        ...jobErrors,
      },
    })

    const fatalError = jobErrors.results[1]
    const { message, fatal } = fatalError
    const component = TestRenderer.create(
      <JobErrorType error={{ message, fatal }} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
