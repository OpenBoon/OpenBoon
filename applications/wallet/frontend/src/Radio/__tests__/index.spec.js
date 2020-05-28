import TestRenderer, { act } from 'react-test-renderer'

import Radio from '..'

describe('<Radio />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Radio
        option={{
          label: 'Radio',
          value: 'radio',
          legend: 'radioLegend',
          initialValue: false,
          isDisabled: false,
        }}
        onClick={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ type: 'radio' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockFn).toHaveBeenCalledWith(true)
  })

  it('should render properly disabled', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Radio
        option={{
          label: 'Radio',
          value: 'radio',
          legend: 'radioLegend',
          initialValue: false,
          isDisabled: true,
        }}
        onClick={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ type: 'radio' }).props.onClick()
    })

    expect(mockFn).not.toHaveBeenCalled()
  })

  it('should render properly checked and disabled', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Radio
        option={{
          label: 'Radio',
          value: 'radio',
          legend: 'radioLegend',
          initialValue: true,
          isDisabled: true,
        }}
        onClick={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ type: 'radio' }).props.onClick()
    })

    expect(mockFn).not.toHaveBeenCalled()
  })
})
