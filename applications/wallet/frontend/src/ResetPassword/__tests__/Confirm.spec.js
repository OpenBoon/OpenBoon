import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import ResetPasswordConfirm, { noop } from '../Confirm'

jest.mock('../../Authentication/helpers', () => ({
  getUser: () => mockUser,
}))

describe('<ResetPasswordConfirm />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)
    require('next/router').__setUseRouter({
      pathname: '/reset-password',
      query: { action: 'enter-new-password' },
    })

    const component = TestRenderer.create(
      <ResetPasswordConfirm uid="GG" token="123" />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Set new password
    act(() => {
      component.root
        .findByProps({ id: 'newPassword' })
        .props.onChange({ target: { value: 'foo' } })

      // Set unmatching confirm password
      component.root
        .findByProps({ id: 'newPassword2' })
        .props.onChange({ target: { value: 'bar' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Fix confirm password to match
    act(() => {
      component.root
        .findByProps({ id: 'newPassword2' })
        .props.onChange({ target: { value: 'foo' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Failure
    fetch.mockResponseOnce(
      JSON.stringify({ newPassword1: ['Error'], newPassword2: ['Error'] }),
      {
        status: 400,
      },
    )

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Password Changed' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalledWith('/?action=enter-new-password-success')
  })

  it('noop should do nothing', () => {
    expect(noop()()).toBe(undefined)
  })
})
