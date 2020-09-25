import TestRenderer, { act } from 'react-test-renderer'

import detections from '../__mocks__/detections'

import TimelineTracks from '../Tracks'

describe('<TimelineTracks />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <TimelineTracks
        videoRef={{}}
        length={18}
        moduleColor="#0074f5"
        predictions={detections[0].predictions}
        isOpen
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should jump to time', () => {
    const mockPause = jest.fn()

    const current = {
      duration: 18,
      currentTime: 0,
      pause: mockPause,
    }

    const component = TestRenderer.create(
      <TimelineTracks
        videoRef={{ current }}
        length={18}
        moduleColor="#0074f5"
        predictions={detections[0].predictions}
        isOpen={false}
      />,
    )

    act(() => {
      act(() => {
        component.root.findByProps({ 'aria-label': '00:00:04' }).props.onClick()
      })

      expect(current).toEqual({ ...current, currentTime: 4 })

      expect(mockPause).toHaveBeenCalledWith()
    })
  })
})
