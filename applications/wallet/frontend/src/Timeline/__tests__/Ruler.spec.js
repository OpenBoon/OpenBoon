import TestRenderer, { act } from 'react-test-renderer'

import TimelineRuler from '../Ruler'

describe('<TimelineRuler />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const videoRef = {
      current: { duration: 18, currentTime: 0, pause: mockFn },
    }

    const rulerRef = {
      current: { scrollLeft: 0 },
    }

    const component = TestRenderer.create(
      <TimelineRuler
        videoRef={videoRef}
        rulerRef={rulerRef}
        length={18}
        width={200}
      />,
    )

    act(() => {})

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      // (a) default ruler width is 500, 50% is 250
      // (b) settings.width is 200
      // clientX is (a) 250 + (b) 200 = 450
      component.root.findAllByType('div')[1].props.onClick({ clientX: 450 })
    })

    expect(mockFn).toHaveBeenCalledWith()

    expect(videoRef.current.currentTime).toBe(9)
  })

  it('should handle infinity clicks', () => {
    const mockFn = jest.fn()

    const videoRef = {
      current: { duration: Infinity, currentTime: 0, pause: mockFn },
    }

    const rulerRef = {
      current: { scrollLeft: 0 },
    }

    const component = TestRenderer.create(
      <TimelineRuler
        videoRef={videoRef}
        rulerRef={rulerRef}
        length={18}
        width={200}
      />,
    )

    act(() => {})

    act(() => {
      component.root.findAllByType('div')[1].props.onClick({ clientX: 450 })
    })

    expect(mockFn).toHaveBeenCalledWith()

    expect(videoRef.current.currentTime).toBe(0)
  })
})
