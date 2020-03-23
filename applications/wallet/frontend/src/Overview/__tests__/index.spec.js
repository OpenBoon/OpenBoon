import TestRenderer from 'react-test-renderer'

import projects from '../../Projects/__mocks__/projects'

import Overview from '..'

jest.mock('../UsagePlan', () => 'OverviewUsagePlan')

describe('<Overview />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: projects })

    const component = TestRenderer.create(<Overview />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
