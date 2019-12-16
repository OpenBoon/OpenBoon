import TestRenderer from 'react-test-renderer'

import Input from '..'

const noop = () => () => {}

describe('<Input />', () => {
  it('should render properly with an error', () => {
    const component = TestRenderer.create(
      <Input
        autoFocus
        id="username"
        label="Email"
        type="text"
        value="foo@bar.baz"
        onChange={noop}
        hasError
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
