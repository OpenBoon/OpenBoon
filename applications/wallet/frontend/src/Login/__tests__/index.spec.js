import TestRenderer, { act } from 'react-test-renderer'

import Login from '..'

const noop = () => () => {}

describe('<Login />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/',
      query: { action: 'password-reset-request-success' },
    })

    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Login
        googleAuth={{ signIn: noop }}
        hasGoogleLoaded
        errorMessage=""
        onSubmit={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    const usernameInput = component.root.findByProps({ id: 'username' })
    const passwordInput = component.root.findByProps({ id: 'password' })

    act(() => {
      usernameInput.props.onChange({ target: { value: 'username' } })
      passwordInput.props.onChange({ target: { value: 'password' } })
    })

    expect(usernameInput.props.value).toEqual('username')
    expect(passwordInput.props.value).toEqual('password')

    act(() => {
      component.root
        .findByProps({ children: 'Login' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalledWith({
      username: 'username',
      password: 'password',
    })
  })

  it('should render properly on account activation success', () => {
    require('next/router').__setUseRouter({
      pathname: '/',
      query: { action: 'account-activation-success' },
    })

    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Login
        googleAuth={{ signIn: noop }}
        hasGoogleLoaded
        errorMessage=""
        onSubmit={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not POST the form', () => {
    const mockFn = jest.fn()
    const mockOnSubmit = jest.fn()

    const component = TestRenderer.create(
      <Login
        googleAuth={{ signIn: noop }}
        hasGoogleLoaded
        errorMessage=""
        onSubmit={mockOnSubmit}
      />,
    )

    component.root
      .findByProps({ method: 'post' })
      .props.onSubmit({ preventDefault: mockFn })

    expect(mockOnSubmit).not.toHaveBeenCalled()
    expect(mockFn).toHaveBeenCalled()
  })

  it('should show and hide the password', () => {
    const component = TestRenderer.create(
      <Login
        googleAuth={{ signIn: noop }}
        hasGoogleLoaded
        errorMessage=""
        onSubmit={noop}
      />,
    )

    expect(component.root.findByProps({ id: 'password' }).props.type).toBe(
      'password',
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Show password' })
        .props.onClick()
    })

    expect(component.root.findByProps({ id: 'password' }).props.type).toBe(
      'text',
    )
  })

  it('should render password changed', () => {
    require('next/router').__setUseRouter({
      pathname: '/',
      query: { action: 'password-reset-update-success' },
    })

    const component = TestRenderer.create(
      <Login
        googleAuth={{ signIn: noop }}
        hasGoogleLoaded
        errorMessage=""
        onSubmit={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render error', () => {
    require('next/router').__setUseRouter({
      pathname: '/',
      query: { action: 'password-reset-update-success' },
    })

    const component = TestRenderer.create(
      <Login
        googleAuth={{ signIn: noop }}
        hasGoogleLoaded
        errorMessage="Something went wrong"
        onSubmit={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
