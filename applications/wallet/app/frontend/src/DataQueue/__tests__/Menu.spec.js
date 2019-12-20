import TestRenderer, { act } from 'react-test-renderer'

import DataQueueMenu from '../Menu'

const noop = () => () => {}

describe('<DataQueueMenu />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <DataQueueMenu
        projectId="76917058-b147-4556-987a-0a0f11e46d9b"
        jobId="82d5308b-67c2-1433-8fef-0a580a000955"
        revalidate={noop}
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
