import TestRenderer, { act } from 'react-test-renderer'

import DataQueueMenu from '../Menu'

describe('<DataQueueMenu />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<DataQueueMenu />)

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
