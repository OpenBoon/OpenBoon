import TestRenderer from 'react-test-renderer'

import ApiKeys from '..'

import apikeys from '../__mocks__/apikeys'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ApiKeys />', () => {
  it('should render properly while loading', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {},
    })

    const component = TestRenderer.create(<ApiKeys />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no api keys', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys',
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

    const component = TestRenderer.create(<ApiKeys />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with api keys', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: apikeys,
    })

    const component = TestRenderer.create(<ApiKeys />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
