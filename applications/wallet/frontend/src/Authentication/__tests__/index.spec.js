import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Authentication, { noop } from '..'

jest.mock('../helpers')

jest.mock('../../Login', () => 'Login')
jest.mock('../../Projects', () => 'Projects')
jest.mock('../../Layout', () => 'Layout')

describe('<Authentication />', () => {
  window.addEventListener = jest.fn((event, cb) => {
    if (event === 'load') {
      cb()
    }
  })

  Object.defineProperty(window, 'gapi', {
    writable: true,
    value: {
      load: (_, cb) => cb(),
      auth2: {
        init: () => ({ signIn: noop, signOut: noop }),
      },
    },
  })

  it('should render properly when user is logged out', async () => {
    const mockFn = jest.fn()

    require('../helpers').__setMockAuthenticateUser(mockFn)

    const component = TestRenderer.create(
      <User initialUser={{}}>
        <Authentication route="/">Hello World!</Authentication>
      </User>,
    )

    // user is loading
    expect(component.toJSON()).toMatchSnapshot()

    // useEffect reads from localStorage
    await act(async () => {})

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
    const component = TestRenderer.create(
      <User initialUser={{}}>
        <Authentication route="/">Hello World!</Authentication>
      </User>,
    )

    // useEffect loads Google SDK
    await act(async () => {})

    expect(component.root.findByType('Login').props.hasGoogleLoaded).toBe(true)
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

  it('noop should do nothing', () => {
    expect(noop()()).toBe(undefined)
  })
})
