import TestRenderer from 'react-test-renderer'

import Toggletip from '..'

describe('<Toggletip />', () => {
  it('should open properly to the left', () => {
    const component = TestRenderer.create(
      <Toggletip openToThe="left" label="Label String">
        Toggle Tip
      </Toggletip>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should open properly to the right', () => {
    const component = TestRenderer.create(
      <Toggletip openToThe="right" label="Label String">
        Toggle Tip
      </Toggletip>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
