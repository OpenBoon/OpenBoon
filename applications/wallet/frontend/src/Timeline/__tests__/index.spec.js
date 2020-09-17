import TestRenderer from 'react-test-renderer'

import Timeline from '..'

jest.mock('../Controls', () => 'TimelineControls')
jest.mock('../Captions', () => 'TimelineCaptions')
jest.mock('../Playhead', () => 'TimelinePlayhead')
jest.mock('../Aggregate', () => 'TimelineAggregate')
jest.mock('../Detections', () => 'TimelineDetections')

describe('<Timeline />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Timeline videoRef={{ current: undefined }} length={18} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
