import TestRenderer, { act } from 'react-test-renderer'

import timelines from '../__mocks__/timelines'

import TimelineTracks from '../Tracks'

describe('<TimelineTracks />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <TimelineTracks
        videoRef={{}}
        length={18}
        color="#009f22"
        tracks={timelines[0].tracks}
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
        color="#009f22"
        tracks={timelines[0].tracks}
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
