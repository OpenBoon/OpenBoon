import TestRenderer from 'react-test-renderer'

import Jobs from '..'

import jobs from '../__mocks__/jobs'

jest.mock('../../Pagination', () => 'Pagination')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Jobs />', () => {
  it('should render properly with no jobs', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(<Jobs />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with jobs', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobs,
    })

    const component = TestRenderer.create(<Jobs />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
