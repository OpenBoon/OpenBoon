import TestRenderer, { act } from 'react-test-renderer'

import detections from '../__mocks__/detections'

import TimelineDetections from '../Detections'

const noop = () => {}

jest.mock('../Tracks', () => 'TimelineTracks')

describe('<TimelineDetections />', () => {
  it('should render properly', () => {
    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <TimelineDetections
        detections={detections}
        settings={{ [detections[0].name]: { isOpen: true } }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': detections[0].name })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      payload: { name: detections[0].name },
      type: 'TOGGLE_OPEN',
    })
  })
})
