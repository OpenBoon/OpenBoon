import TestRenderer from 'react-test-renderer'

import projects from '../../Projects/__mocks__/projects'

import Account from '..'

jest.mock('../UsagePlan', () => 'AccountUsagePlan')

describe('<Account />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: projects })

    const component = TestRenderer.create(<Account />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no projects', () => {
    require('swr').__setMockUseSWRResponse({ data: { results: [] } })

    const component = TestRenderer.create(<Account />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
