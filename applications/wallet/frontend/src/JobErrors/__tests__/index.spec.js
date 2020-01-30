import TestRenderer from 'react-test-renderer'

import JobErrors from '..'

import jobErrors from '../__mocks__/jobErrors'

jest.mock('../../Pagination', () => 'Pagination')
jest.mock('../../UserMenu', () => 'UserMenu')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = 'c097596f-62ef-1f81-83f8-0a580a000954'

describe('<JobErrors />', () => {
  it('should render properly while loading', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors',
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {},
    })

    const component = TestRenderer.create(<JobErrors />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no job errors', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(<JobErrors />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with job errors', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors',
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrors,
    })

    const component = TestRenderer.create(<JobErrors />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
