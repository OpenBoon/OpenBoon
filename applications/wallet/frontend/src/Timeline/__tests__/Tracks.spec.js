import TestRenderer from 'react-test-renderer'

import TimelineTracks from '../Tracks'

describe('<TimelineTracks />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <TimelineTracks
        name="gcp-video-explicit-detection"
        predictions={[{ label: 'Ghost' }]}
        isOpen
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
