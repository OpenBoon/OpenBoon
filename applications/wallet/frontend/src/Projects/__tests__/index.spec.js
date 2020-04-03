import TestRenderer, { act } from 'react-test-renderer'

import projects from '../__mocks__/projects'

import Projects from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

const noop = () => () => {}

describe('<Projects />', () => {
  it('should render properly with projects', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <Projects projectId={PROJECT_ID} mutate={noop}>
        Hello World
      </Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render Account Overview properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/',
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <Projects projectId="" mutate={noop}>
        Account Overview
      </Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should update the projectId if it is different from the router projectId', async () => {
    let user = {}

    const mockFn = jest.fn((cb) => {
      user = cb(user)
    })

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    TestRenderer.create(
      <Projects projectId="not-the-same-project-id" mutate={mockFn}>
        Hello World
      </Projects>,
    )

    // useEffect
    await act(async () => {})

    expect(user).toEqual({ projectId: PROJECT_ID })
  })

  it('should redirect if there is no project id and the route requires a project id', () => {
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
      <Projects projectId="" mutate={noop}>
        Hello World
      </Projects>,
    )

    expect(mockFn).toHaveBeenCalledWith(
      '/[projectId]/jobs',
      '/76917058-b147-4556-987a-0a0f11e46d9b/jobs',
    )

    expect(component.toJSON()).toBeNull()
  })

  it('should not redirect if there is no project id and the route does not require a project id', () => {
    require('next/router').__setUseRouter({
      pathname: '/account',
      query: {},
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <Projects projectId="" mutate={noop}>
        Hello World
      </Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should redirect if the url projectId is not of an authorized project', () => {
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
      <Projects projectId="" mutate={noop}>
        Hello World
      </Projects>,
    )

    expect(mockFn).toHaveBeenCalledWith(
      '/[projectId]/jobs',
      '/76917058-b147-4556-987a-0a0f11e46d9b/jobs',
    )

    expect(component.toJSON()).toBeNull()
  })

  it('should redirect if there is a projectId but no project', () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: 'not-a-valid-project-id' },
    })

    require('swr').__setMockUseSWRResponse({
      data: { results: [] },
    })

    const component = TestRenderer.create(
      <Projects projectId="" mutate={noop}>
        Hello World
      </Projects>,
    )

    expect(mockFn).toHaveBeenCalledWith('/')

    expect(component.toJSON()).toBeNull()
  })

  it('should reset an invalid user projectId', async () => {
    let user = {}

    const mockMutate = jest.fn((cb) => {
      user = cb(user)
    })

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: 'not-a-valid-project-id' },
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <Projects projectId="not-a-valid-project-id" mutate={mockMutate}>
        Hello World
      </Projects>,
    )

    expect(user).toEqual({ projectId: '' })

    expect(mockRouterPush).not.toHaveBeenCalled()

    expect(component.toJSON()).toBeNull()
  })
})
