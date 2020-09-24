import TestRenderer from 'react-test-renderer'

import FlashMessageErrors from '../Errors'

describe('<FlashMessageErrors />', () => {
  it('should render with a global error', () => {
    const component = TestRenderer.create(
      <FlashMessageErrors
        errors={{ global: 'Something went wrong. Please try again.' }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render null without a global error', () => {
    const component = TestRenderer.create(<FlashMessageErrors errors={{}} />)

    expect(component.toJSON()).toEqual(null)
  })
})
