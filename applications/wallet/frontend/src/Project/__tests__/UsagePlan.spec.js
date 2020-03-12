import TestRenderer from 'react-test-renderer'

import project from '../__mocks__/project'
import subscriptions from '../../Subscriptions/__mocks__/subscriptions'

import ProjectUsagePlan from '../UsagePlan'

describe('<ProjectUsagePlan />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]',
      query: { projectId: project.id },
    })

    require('swr').__setMockUseSWRResponse({ data: subscriptions })

    const component = TestRenderer.create(<ProjectUsagePlan />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not render without subscriptions', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]',
      query: { projectId: project.id },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(<ProjectUsagePlan />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
