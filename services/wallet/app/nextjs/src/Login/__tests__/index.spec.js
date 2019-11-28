import TestRenderer, { act } from 'react-test-renderer'

import Login from '../'

const noop = () => () => {}

describe('<Login />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Login onSubmit={noop} />)

    expect(component.toJSON()).toMatchSnapshot()

    const usernameInput = component.root.findByProps({ id: 'username' })
    const passwordInput = component.root.findByProps({ id: 'password' })

    act(() => {
      usernameInput.props.onChange({ target: { value: 'foo@bar.baz' } })
      passwordInput.props.onChange({ target: { value: 'password' } })
    })

    expect(usernameInput.props.value).toEqual('foo@bar.baz')
    expect(passwordInput.props.value).toEqual('password')
  })
})
