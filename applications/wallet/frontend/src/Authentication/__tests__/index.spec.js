import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Authentication, { noop } from '..'

jest.mock('../helpers')

jest.mock('../../Login', () => 'Login')
jest.mock('../../Projects', () => 'Projects')
jest.mock('../../Layout', () => 'Layout')

describe('<Authentication />', () => {
  it('should render properly when user is logged out', async () => {
    const mockFn = jest.fn()

    require('../helpers').__setMockAuthenticateUser(mockFn)

    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <User initialUser={{}}>
        <Authentication route="/">Hello World!</Authentication>
      </User>,
    )

    // display <Login />
    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Login').props.onSubmit({
        username: 'username',
        password: 'password',
      })
    })

    expect(mockFn).toHaveBeenCalledWith({
      username: 'username',
      password: 'password',
    })
  })

  it('should render properly if user is logged out on AUTHENTICATION_LESS_ROUTES', async () => {
    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <User initialUser={{}}>
        <Authentication route="/create-account">Hello World!</Authentication>
      </User>,
    )

    // useEffect reads from localStorage
    await act(async () => {})

    // Expect Hello World!
    expect(component.toJSON()).toEqual('Hello World!')
  })

  it('should load the Google SDK', async () => {
    Object.defineProperty(window, 'gapi', {
      writable: true,
      value: {
        load: (_, cb) => cb(),
        auth2: {
          init: () =>
            new Promise((resolve, reject) => {
              resolve({ signIn: noop, signOut: noop })
              reject()
            }),
        },
      },
    })

    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <User initialUser={{}}>
        <Authentication route="/">Hello World!</Authentication>
      </User>,
    )

    // useEffect loads Google SDK
    await act(async () => {})

    expect(component.root.findByType('Login').props.hasGoogleLoaded).toBe(true)

    window.gapi = undefined
  })

  it('should render properly when user needs to approve new policies', async () => {
    const component = TestRenderer.create(
      <User initialUser={{ ...mockUser, agreedToPoliciesDate: '00000000' }}>
        <Authentication route="/">Hello World!</Authentication>
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when user is logged in', async () => {
    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Authentication route="/">Hello World!</Authentication>
      </User>,
    )

    // display `Hello World!`
    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when user is logged in on AUTHENTICATION_LESS_ROUTES', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Authentication route="/create-account">Hello World!</Authentication>
      </User>,
    )

    expect(mockRouterPush).toHaveBeenCalledWith('/')

    expect(component.toJSON()).toEqual(null)
  })

  it('should render properly when the Google SDK is blocked', async () => {
    window.gapi = undefined

    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <User initialUser={{}}>
        <Authentication route="/">Hello World!</Authentication>
      </User>,
    )

    // useEffect loads Google SDK
    await act(async () => {})

    expect(component.root.findByType('Login').props.hasGoogleLoaded).toBe(false)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('noop should do nothing', () => {
    expect(noop()()).toBe(undefined)
  })
})
