import TestRenderer, { act } from 'react-test-renderer'

import ResetPassword from '..'

jest.mock('../Confirm', () => 'ResetPasswordConfirm')

const noop = () => () => {}

describe('<ResetPassword />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    require('next/router').__setUseRouter({
      pathname: '/reset-password',
      query: {},
    })
    require('next/router').__setMockPushFunction(mockFn)

    const component = TestRenderer.create(<ResetPassword />)

    expect(component.toJSON()).toMatchSnapshot()

    const usernameInput = component.root.findByProps({ id: 'email' })

    // Enter email
    act(() => {
      usernameInput.props.onChange({ target: { value: 'jane@zorroa.com' } })
    })

    // Mock Failure
    fetch.mockRejectOnce({ error: 'Invalid' }, { status: 400 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Request Reset Email' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ email: 'username' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Request Reset Email' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(2)

    expect(fetch.mock.calls[0][0]).toEqual(`/api/v1/password/reset/`)

    expect(mockFn).toHaveBeenCalledWith(
      '/?action=password-reset-request-success',
      '/',
    )
  })

  it('should not POST the form', () => {
    const mockFn = jest.fn()
    const mockOnSubmit = jest.fn()

    const component = TestRenderer.create(<ResetPassword />)

    component.root
      .findByProps({ method: 'post' })
      .props.onSubmit({ preventDefault: mockFn })

    expect(mockOnSubmit).not.toHaveBeenCalled()
    expect(mockFn).toHaveBeenCalled()
  })

  it('should render <ResetPasswordConfirm />', () => {
    require('next/router').__setUseRouter({
      pathname: '/reset-password',
      query: { uid: 'GG', token: '123' },
    })

    const component = TestRenderer.create(<ResetPassword />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
