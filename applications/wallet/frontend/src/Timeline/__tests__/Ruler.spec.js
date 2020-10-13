import TestRenderer, { act } from 'react-test-renderer'

import TimelineRuler from '../Ruler'

describe('<TimelineRuler />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<TimelineRuler length={100} />)

    act(() => {})

    expect(component.toJSON()).toMatchSnapshot()
  })
})
