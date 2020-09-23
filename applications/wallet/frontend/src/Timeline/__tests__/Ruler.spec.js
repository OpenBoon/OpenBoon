import TestRenderer from 'react-test-renderer'

import TimelineRuler from '../Ruler'

describe('<TimelineRuler />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<TimelineRuler />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
