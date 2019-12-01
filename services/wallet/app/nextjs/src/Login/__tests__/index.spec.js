import TestRenderer, { act } from 'react-test-renderer'

import Login from '../'

const noop = () => () => {}

describe('<Login />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Login onSubmit={noop} />)

    expect(component.toJSON()).toMatchSnapshot()

    const emailInput = component.root.findByProps({ id: 'email' })
    const passwordInput = component.root.findByProps({ id: 'password' })

    act(() => {
      emailInput.props.onChange({ target: { value: 'foo@bar.baz' } })
      passwordInput.props.onChange({ target: { value: 'password' } })
    })

    expect(emailInput.props.value).toEqual('foo@bar.baz')
    expect(passwordInput.props.value).toEqual('password')
  })
})
