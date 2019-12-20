import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import projects from '../__mocks__/projects'

import Projects from '..'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Projects />', () => {
  it('should render properly while loading', () => {
    const component = TestRenderer.create(
      <Projects user={mockUser} logout={noop}>
        {() => `Hello World`}
      </Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no projects', () => {
    require('swr').__setMockUseSWRResponse({
      data: { results: [] },
    })

    const component = TestRenderer.create(
      <Projects user={mockUser} logout={noop}>
        {() => `Hello World`}
      </Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with projects', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <Projects user={mockUser} logout={noop}>
        {() => `Hello World`}
      </Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should redirect if theres no project id', () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: {},
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <Projects user={mockUser} logout={noop}>
        {() => `Hello World`}
      </Projects>,
    )

    expect(mockFn).toHaveBeenCalledWith(
      '/[projectId]/jobs',
      '/76917058-b147-4556-987a-0a0f11e46d9b/jobs',
    )

    expect(component.toJSON()).toBeNull()
  })

  it('should redirect if the projectId is not of an authorized project', () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: 'not-a-valid-project-id' },
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <Projects user={mockUser} logout={noop}>
        {() => `Hello World`}
      </Projects>,
    )

    expect(mockFn).toHaveBeenCalledWith(
      '/[projectId]/jobs',
      '/76917058-b147-4556-987a-0a0f11e46d9b/jobs',
    )

    expect(component.toJSON()).toBeNull()
  })
})
