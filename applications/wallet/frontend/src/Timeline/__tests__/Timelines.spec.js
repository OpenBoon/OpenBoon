import TestRenderer, { act } from 'react-test-renderer'

import timelines from '../__mocks__/timelines'

import TimelineTimelines from '../Timelines'

const noop = () => {}

jest.mock('../Tracks', () => 'TimelineTracks')

describe('<TimelineTimelines />', () => {
  it('should render properly', () => {
    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <TimelineTimelines
        videoRef={{ current: { duration: 18 } }}
        length={18}
        timelines={timelines}
        settings={{
          width: 200,
          filter: '',
          timelines: {
            [timelines[0].timeline]: { isOpen: true, color: '#0074f5' },
            [timelines[1].timeline]: { isOpen: false, color: '#a03dc7' },
            [timelines[2].timeline]: { isOpen: false, color: '#ebb52e' },
            [timelines[3].timeline]: { isOpen: false, color: '#d6680b' },
            [timelines[4].timeline]: { isOpen: false, color: '#00bdc1' },
          },
          zoom: 100,
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': timelines[0].timeline })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      payload: { timeline: timelines[0].timeline },
      type: 'TOGGLE_OPEN',
    })
  })
})
