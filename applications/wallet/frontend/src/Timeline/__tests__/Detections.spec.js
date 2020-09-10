import TestRenderer, { act } from 'react-test-renderer'

import TimelineDetections from '../Detections'

describe('<TimelineDetections />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<TimelineDetections />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findAllByType('details')[0]
        .props.onToggle({ target: { open: true } })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
