import TestRenderer from 'react-test-renderer'

import apiKeys from '../__mocks__/apiKeys'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import ApiKeys from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ApiKeys />', () => {
  it('should render properly with no api keys', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys',
      query: { projectId: PROJECT_ID, action: 'delete-apikey-success' },
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
        <ApiKeys />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with api keys', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: apiKeys,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ApiKeys />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
