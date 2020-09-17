import TestRenderer, { act } from 'react-test-renderer'

import detections from '../__mocks__/detections'

import TimelineDetections from '../Detections'

jest.mock('../Tracks', () => 'TimelineTracks')

const noop = () => () => {}

describe('<TimelineDetections />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <TimelineDetections detections={detections} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'gcp-video-explicit-detection' })
        .props.onClick({ target: { open: true }, preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
