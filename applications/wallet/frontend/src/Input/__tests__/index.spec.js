import TestRenderer from 'react-test-renderer'

import Input, { VARIANTS } from '..'

import { noop } from '../Range'

describe('<Input />', () => {
  it('should render properly with an error and disabled', () => {
    const component = TestRenderer.create(
      <Input
        autoFocus
        id="username"
        variant={VARIANTS.PRIMARY}
        label="Email"
        type="text"
        value="foo@bar.baz"
        onChange={noop}
        hasError
        errorMessage="Error"
        isDisabled
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('noop should do nothing', () => {
    expect(noop()()).toBe(undefined)
  })
})
