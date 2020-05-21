import TestRenderer from 'react-test-renderer'

import FilterRangeSlider from '../Slider'

const noop = () => () => {}

describe('<FilterRangeSlider />', () => {
  it('should render properly when muted', () => {
    const component = TestRenderer.create(
      <FilterRangeSlider
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
