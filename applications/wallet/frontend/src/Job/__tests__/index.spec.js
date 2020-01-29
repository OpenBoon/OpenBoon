import TestRenderer from 'react-test-renderer'

import Job from '..'

jest.mock('../../Pagination', () => 'Pagination')
jest.mock('../../UserMenu', () => 'UserMenu')
jest.mock('../../Breadcrumbs', () => 'Breadcrumbs')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '82d5308b-67c2-1433-8fef-0a580a000956'

describe('<Job />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/details',
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {},
    })

    const component = TestRenderer.create(<Job />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
