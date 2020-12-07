import TestRenderer, { act } from 'react-test-renderer'

import CheckboxSwitch from '../Switch'

const noop = () => {}

describe('<Checkbox />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <CheckboxSwitch
        option={{
          value: 'checkbox',
          label: 'Checkbox',
          initialValue: false,
          isDisabled: false,
        }}
        onClick={noop}
      />,
    )

    // Focus
    act(() => {
      component.root
        .findByProps({ type: 'checkbox' })
        .props.onFocus({ target: { className: 'focus-visible' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Check
    act(() => {
      component.root.findByProps({ type: 'checkbox' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Blur
    act(() => {
      component.root.findByProps({ type: 'checkbox' }).props.onBlur()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
