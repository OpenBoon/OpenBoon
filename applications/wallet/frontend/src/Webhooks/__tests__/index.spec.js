import TestRenderer from 'react-test-renderer'

import webhooks from '../__mocks__/webhooks'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Webhooks from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Webhooks />', () => {
  it('should render properly with no webhook', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys',
      query: { projectId: PROJECT_ID, action: 'delete-webhook-success' },
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
      <User initialUser={mockUser}>
        <Webhooks />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with webhooks', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/webhooks',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: webhooks })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Webhooks />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
