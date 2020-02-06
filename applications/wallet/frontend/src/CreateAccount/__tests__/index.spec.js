import TestRenderer, { act } from 'react-test-renderer'

import CreateAccount, { noop } from '..'

describe('<CreateAccount />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

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

    // Mock Failure
    fetch.mockResponseOnce('Invalid email', { status: 400 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    // Dismiss Error Message
    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Close alert' })
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

    expect(mockFn).toHaveBeenCalledWith('/?action=create-account-success')
  })

  it('noop should do nothing', () => {
    expect(noop()()).toBe(undefined)
  })
})
