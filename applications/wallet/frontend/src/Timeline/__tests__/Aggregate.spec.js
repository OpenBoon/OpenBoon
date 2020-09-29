import TestRenderer, { act } from 'react-test-renderer'

import timelines from '../__mocks__/timelines'

import TimelineAggregate from '../Aggregate'

jest.mock('../Tracks', () => 'TimelineTracks')

describe('<TimelineAggregate />', () => {
  it('should render properly', () => {
    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <TimelineAggregate
        timelineHeight={400}
        timelines={timelines}
        settings={{
          filter: '',
          modules: { [timelines[0].timeline]: { isVisible: true } },
        }}
        dispatch={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Dropdown Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ value: 'all' }).props.onClick()
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      payload: { timelines },
      type: 'TOGGLE_VISIBLE_ALL',
    })

    act(() => {
      component.root
        .findByProps({ value: timelines[0].timeline })
        .props.onClick()
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      payload: { timeline: timelines[0].timeline },
      type: 'TOGGLE_VISIBLE',
    })
  })
})
