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
          filter: '',
          modules: { [timelines[0].timeline]: { isOpen: true } },
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
