import TestRenderer, { act } from 'react-test-renderer'

import detections from '../__mocks__/detections'

import TimelineAggregate from '../Aggregate'

jest.mock('../Tracks', () => 'TimelineTracks')

describe('<TimelineAggregate />', () => {
  it('should render properly', () => {
    const mockDispatch = jest.fn()

    const component = TestRenderer.create(
      <TimelineAggregate
        timelineHeight={400}
        detections={detections}
        settings={{
          filter: '',
          modules: { [detections[0].name]: { isVisible: true } },
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
      payload: { detections },
      type: 'TOGGLE_VISIBLE_ALL',
    })

    act(() => {
      component.root.findByProps({ value: detections[0].name }).props.onClick()
    })

    expect(mockDispatch).toHaveBeenCalledWith({
      payload: { name: detections[0].name },
      type: 'TOGGLE_VISIBLE',
    })
  })
})
