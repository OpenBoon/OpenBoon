import TestRenderer from 'react-test-renderer'

import ToggleTip from '..'

describe('<ToggleTip />', () => {
  it('should open properly to the right', () => {
    const component = TestRenderer.create(
      <ToggleTip openToThe="right">Toggle Tip</ToggleTip>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
