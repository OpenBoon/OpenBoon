import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import Authentication, { noop } from '..'

jest.mock('../../Login', () => 'Login')
jest.mock('../../Projects', () => 'Projects')
jest.mock('../../Layout', () => 'Layout')

jest.mock('../helpers')

describe('<Authentication />', () => {
  Object.defineProperty(window, 'onload', {
    set: cb => cb(),
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
      <Authentication>{({ user }) => `Hello ${user.email}!`}</Authentication>,
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

  it('should load the Google SDK', async () => {
    const component = TestRenderer.create(
      <Authentication>{() => 'Hello World'}</Authentication>,
    )

    // useEffect loads Google SDK
    await act(async () => {})

    expect(component.root.findByType('Login').props.hasGoogleLoaded).toBe(true)
  })

  it('should render properly when user is logged in', async () => {
    require('../helpers').__setMockUser(mockUser)

    const component = TestRenderer.create(
      <Authentication>{() => 'Hello World'}</Authentication>,
    )

    // user is loading
    expect(component.toJSON()).toMatchSnapshot()

    // useEffect reads from localStorage
    await act(async () => {})

    // display `Hello World!`
    expect(component.toJSON()).toMatchSnapshot()

    // reset localStorage
    require('../helpers').__setMockUser({})
  })

  it('noop should do nothing', () => {
    expect(noop()()).toBe(undefined)
  })
})
