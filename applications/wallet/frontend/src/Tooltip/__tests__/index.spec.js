import TestRenderer, { act } from 'react-test-renderer'

import Tooltip from '..'

describe('<Tooltip />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Tooltip content="floaty stuff">Hello</Tooltip>,
    )

    act(() => {
      component.root.findAllByProps({ children: 'Hello' })[1].props.onFocus()
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findAllByProps({ children: 'Hello' })[1].props.onBlur()
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findAllByProps({ children: 'Hello' })[1]
        .props.onMouseEnter()
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findAllByProps({ children: 'Hello' })[1]
        .props.onMouseLeave()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
