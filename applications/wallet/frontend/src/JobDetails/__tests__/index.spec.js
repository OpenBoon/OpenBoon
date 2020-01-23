import TestRenderer from 'react-test-renderer'

import JobDetails from '..'

jest.mock('../../Pagination', () => 'Pagination')
jest.mock('../../UserMenu', () => 'UserMenu')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '82d5308b-67c2-1433-8fef-0a580a000956'

describe('<JobDetails />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/details',
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {},
    })

    const component = TestRenderer.create(<JobDetails />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
