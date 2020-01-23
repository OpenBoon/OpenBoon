import TestRenderer from 'react-test-renderer'

import JobDetails from '..'

jest.mock('../../Pagination', () => 'Pagination')
jest.mock('../../UserMenu', () => 'UserMenu')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<JobDetails />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/details',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {},
    })

    const component = TestRenderer.create(<JobDetails />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
