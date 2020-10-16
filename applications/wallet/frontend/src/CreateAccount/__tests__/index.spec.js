import TestRenderer, { act } from 'react-test-renderer'

import CreateAccount, { noop } from '..'

describe('<CreateAccount />', () => {
  it('should render properly when there is a token', async () => {
    require('next/router').__setUseRouter({
      pathname: '/create-account',
      query: {
        token: 'f1c5b71f-bc9d-4b54-aa69-cbec03f94f5e',
        uid: 2,
      },
    })

    const component = TestRenderer.create(<CreateAccount />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when the activation link is expired', () => {
    require('next/router').__setUseRouter({
      pathname: '/create-account',
      query: {
        action: 'account-activation-expired',
      },
    })

    const component = TestRenderer.create(<CreateAccount />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when there is no token', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)
    require('next/router').__setUseRouter({
      pathname: '/create-account',
      query: {},
    })

    const component = TestRenderer.create(<CreateAccount />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ name: 'firstName' })
        .props.onChange({ target: { value: 'Jane' } })
    })

    act(() => {
      component.root
        .findByProps({ name: 'lastName' })
        .props.onChange({ target: { value: 'Doe' } })
    })

    act(() => {
      component.root
        .findByProps({ name: 'email' })
        .props.onChange({ target: { value: 'jane@zorroa.com' } })
    })

    act(() => {
      component.root
        .findByProps({ name: 'password' })
        .props.onChange({ target: { value: 'MyAwesomePassword' } })
    })

    act(() => {
      component.root
        .findByProps({ name: 'confirmPassword' })
        .props.onChange({ target: { value: 'MyAwesomePassword' } })
    })

    act(() => {
      component.root.findByProps({ type: 'checkbox' }).props.onClick()
    })

    // Mock Failure
    fetch.mockResponseOnce('Invalid email', { status: 400 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Account Created' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalledWith('/create-account-success', '/')
  })

  it('noop should do nothing', () => {
    expect(noop()()).toBe(undefined)
  })
})
