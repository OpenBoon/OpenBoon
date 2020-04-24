import TestRenderer, { act } from 'react-test-renderer'

import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '..'

describe('<Checkbox />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Checkbox
        variant={CHECKBOX_VARIANTS.MULTILINE}
        option={{
          value: 'checkbox',
          label: 'Checkbox',
          icon: '',
          legend: '',
          initialValue: false,
          isDisabled: false,
        }}
        onClick={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ type: 'checkbox' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockFn).toHaveBeenCalledWith(true)
  })

  it('should render properly disabled', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Checkbox
        variant={CHECKBOX_VARIANTS.MULTILINE}
        option={{
          value: 'checkbox',
          label: 'Checkbox',
          icon: '',
          legend: '',
          initialValue: false,
          isDisabled: true,
        }}
        onClick={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ type: 'checkbox' }).props.onClick()
    })

    expect(mockFn).not.toHaveBeenCalled()
  })

  it('should render properly checked and disabled', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Checkbox
        variant={CHECKBOX_VARIANTS.MULTILINE}
        option={{
          value: 'checkbox',
          label: 'Checkbox',
          icon: '',
          legend: '',
          initialValue: true,
          isDisabled: true,
        }}
        onClick={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ type: 'checkbox' }).props.onClick()
    })

    expect(mockFn).not.toHaveBeenCalled()
  })
})
