import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import projects from '../__mocks__/projects'

import User from '../../User'
import Projects from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

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
      <Projects projectId={PROJECT_ID}>Hello World</Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render All Projects properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/',
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    const component = TestRenderer.create(
      <Projects projectId="">All Projects</Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should update the projectId if it is different from the router projectId', async () => {
    const mockMutate = jest.fn()

    require('swr').__setMockMutateFn(mockMutate)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: projects,
    })

    TestRenderer.create(
      <Projects projectId="not-the-same-project-id">Hello World</Projects>,
    )

    // useEffect
    await act(async () => {})

    expect(mockMutate).toHaveBeenCalledWith({ projectId: PROJECT_ID })
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
      <Projects projectId="">Hello World</Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render an access denied screen if the url projectId is not of an authorized project', () => {
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
      <User initialUser={mockUser}>
        <Projects projectId="">Hello World</Projects>
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should redirect if there is a projectId but no project', () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: 'not-a-valid-project-id' },
    })

    require('swr').__setMockUseSWRResponse({
      data: {},
    })

    const component = TestRenderer.create(
      <Projects projectId="">Hello World</Projects>,
    )

    expect(mockFn).toHaveBeenCalledWith('/')

    expect(component.toJSON()).toBeNull()
  })
})
