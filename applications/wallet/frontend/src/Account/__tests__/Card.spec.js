import TestRenderer from 'react-test-renderer'

import project from '../../Project/__mocks__/project'
import subscriptions from '../../Subscriptions/__mocks__/subscriptions'

import AccountCard from '../Card'

const PROJECT_ID = project.id
const PROJECT_NAME = project.name

describe('<AccountCard />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: subscriptions })

    const component = TestRenderer.create(
      <AccountCard projectId={PROJECT_ID} name={PROJECT_NAME} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with over usage', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        ...subscriptions,
        results: [
          {
            ...subscriptions.results[0],
            usage: {
              videoHours: 320,
              imageCount: 30040,
            },
          },
        ],
      },
    })

    const component = TestRenderer.create(
      <AccountCard projectId={PROJECT_ID} name={PROJECT_NAME} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not render without subscriptions', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(
      <AccountCard projectId={PROJECT_ID} name={PROJECT_NAME} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
