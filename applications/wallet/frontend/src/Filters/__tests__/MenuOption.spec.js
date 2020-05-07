import TestRenderer from 'react-test-renderer'

import FiltersMenuOption from '../MenuOption'

const noop = () => () => {}

describe('<FiltersMenuOption />', () => {
  it('should render properly', () => {
    const filters = [{ attribute: 'clip.length', type: 'range' }]

    const component = TestRenderer.create(
      <FiltersMenuOption
        option="clip.length"
        label="length"
        filters={filters}
        onClick={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
