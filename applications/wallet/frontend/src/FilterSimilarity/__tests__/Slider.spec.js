import TestRenderer from 'react-test-renderer'

import FilterSimilaritySlider from '../Slider'

const noop = () => () => {}

describe('<FilterSimilaritySlider />', () => {
  it('should render properly when muted', () => {
    const component = TestRenderer.create(
      <FilterSimilaritySlider
        step={0.01}
        domain={[0, 1]}
        values={[0.75]}
        isDisabled
        onUpdate={noop}
        onChange={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
