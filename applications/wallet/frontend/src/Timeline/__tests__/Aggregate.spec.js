import TestRenderer, { act } from 'react-test-renderer'

import detections from '../__mocks__/detections'

import TimelineAggregate, { noop } from '../Aggregate'

jest.mock('../Tracks', () => 'TimelineTracks')

describe('<TimelineAggregate />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <TimelineAggregate detections={detections} timelineHeight={400} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Dropdown Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
