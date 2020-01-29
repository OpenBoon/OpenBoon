import TestRenderer from 'react-test-renderer'

import ProjectUsers from '..'

import projectUsers from '../__mocks__/projectUsers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ProjectUsers />', () => {
  it('should render properly while loading', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {},
    })

    const component = TestRenderer.create(<ProjectUsers />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no project users', () => {
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

    const component = TestRenderer.create(<ProjectUsers />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with project users', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: projectUsers,
    })

    const component = TestRenderer.create(<ProjectUsers />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
