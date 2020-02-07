import TestRenderer, { act } from 'react-test-renderer'

import ResetPassword from '..'

jest.mock('../helpers')

const noop = () => () => {}

describe('<ResetPassword />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/reset-password',
      query: {},
    })

    const component = TestRenderer.create(<ResetPassword />)

    expect(component.toJSON()).toMatchSnapshot()

    const usernameInput = component.root.findByProps({ id: 'username' })

    act(() => {
      usernameInput.props.onChange({ target: { value: 'username' } })
    })

    expect(usernameInput.props.value).toEqual('username')

    act(() => {
      component.root
        .findByProps({ children: 'Request Reset Email' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
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
})
