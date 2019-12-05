import TestRenderer, { act } from 'react-test-renderer'

import Authentication from '..'

jest.mock('../../Login', () => 'Login')

jest.mock('../helpers')

describe('<Authentication />', () => {
  it('should render properly when user is logged out', () => {
    const mockFn = jest.fn()
    require('../helpers').__setMockAuthenticateUser(mockFn)

    const component = TestRenderer.create(
      <Authentication>{({ user }) => `Hello ${user.email}!`}</Authentication>,
    )

    // user is loading
    expect(component.toJSON()).toMatchSnapshot()

    // useEffect reads from localStorage
    act(() => {})

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

  it('should render properly when user is logged in', () => {
    require('../helpers').__setMockTokens({
      accessToken: true,
      refreshToken: true,
    })

    const component = TestRenderer.create(
      <Authentication>{() => `Hello World!`}</Authentication>,
    )

    // user is loading
    expect(component.toJSON()).toMatchSnapshot()

    // useEffect reads from localStorage
    act(() => {})

    // display `Hello World!`
    expect(component.toJSON()).toMatchSnapshot()
  })
})
