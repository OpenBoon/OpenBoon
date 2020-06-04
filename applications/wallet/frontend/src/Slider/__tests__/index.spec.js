import TestRenderer from 'react-test-renderer'

import Slider from '..'

const noop = () => () => {}

describe('<Slider />', () => {
  it('should render properly when muted', () => {
    const component = TestRenderer.create(
      <Slider
        step={0.1}
        domain={[0, 100]}
        values={[0, 100]}
        isDisabled
        onUpdate={noop}
        onChange={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
