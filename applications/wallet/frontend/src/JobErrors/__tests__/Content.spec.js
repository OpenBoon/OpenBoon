import TestRenderer from 'react-test-renderer'

import JobErrorsContent from '../Content'

import job from '../../Job/__mocks__/job'

jest.mock('../Table', () => 'JobErrorsTable')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = 'c097596f-62ef-1f81-83f8-0a580a000954'

describe('<JobErrorsContent />', () => {
  require('next/router').__setUseRouter({
    pathname: '/[projectId]/jobs/[jobId]/errors',
    query: { projectId: PROJECT_ID, jobId: JOB_ID },
  })

  it('should render properly while loading', () => {
    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(<JobErrorsContent />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no job', () => {
    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(<JobErrorsContent />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a job', () => {
    require('swr').__setMockUseSWRResponse({
      data: job,
    })

    const component = TestRenderer.create(<JobErrorsContent />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
