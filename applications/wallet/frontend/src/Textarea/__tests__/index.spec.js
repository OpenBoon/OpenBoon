import TestRenderer from 'react-test-renderer'

import Textarea, { VARIANTS } from '..'

const noop = () => () => {}

describe('<Textarea />', () => {
  it('should render properly with an error', () => {
    const component = TestRenderer.create(
      <Textarea
        autoFocus
        id="username"
        variant={VARIANTS.PRIMARY}
        label="Email"
        value="foo@bar.baz"
        onChange={noop}
        hasError
        errorMessage="Error"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no error', () => {
    const component = TestRenderer.create(
      <Textarea
        autoFocus
        id="username"
        variant={VARIANTS.PRIMARY}
        label="Email"
        value="foo@bar.baz"
        onChange={noop}
        hasError={false}
        errorMessage="Error"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
