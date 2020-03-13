import TestRenderer from 'react-test-renderer'

import project from '../__mocks__/project'

import Project from '..'

jest.mock('../UsagePlan', () => 'ProjectUsagePlan')

describe('<Project />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]',
      query: { projectId: project.id },
    })

    require('swr').__setMockUseSWRResponse({ data: project })

    const component = TestRenderer.create(<Project />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
