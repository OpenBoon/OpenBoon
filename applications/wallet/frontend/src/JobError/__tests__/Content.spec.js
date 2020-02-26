import TestRenderer from 'react-test-renderer'

import JobErrorContent from '..'

import { jobErrorFatal, jobErrorNonFatal } from '../__mocks__/jobError'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ERROR_ID = '916c86bc-74b9-1519-b065-d2f0132bc0c8'
const FATAL_ERROR_ID = '916c86bb-74b9-1519-b065-d2f0132bc0c8'

describe('<JobErrorContent />', () => {
  it('should render properly when fetching error', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, errorId: ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(<JobErrorContent />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a non-fatal error', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, errorId: ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrorNonFatal,
    })

    const component = TestRenderer.create(<JobErrorContent />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a fatal error', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, errorId: FATAL_ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrorFatal,
    })

    const component = TestRenderer.create(<JobErrorContent />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
