import TestRenderer, { act } from 'react-test-renderer'

import Login from '..'

const noop = () => () => {}

describe('<Login />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(<Login onSubmit={mockFn} />)

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
})
