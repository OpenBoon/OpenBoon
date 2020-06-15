import TestRenderer from 'react-test-renderer'

import jobErrors from '../__mocks__/jobErrors'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import JobErrors from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = 'c097596f-62ef-1f81-83f8-0a580a000954'

describe('<JobErrors />', () => {
  it('should render properly with job errors', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors',
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrors,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <JobErrors
          parentUrl={`/api/v1/projects/${PROJECT_ID}/jobs/${JOB_ID}/`}
        />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly without job errors', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors',
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {},
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <JobErrors
          parentUrl={`/api/v1/projects/${PROJECT_ID}/jobs/${JOB_ID}/`}
        />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
