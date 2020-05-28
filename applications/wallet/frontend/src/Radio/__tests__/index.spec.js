import TestRenderer, { act } from 'react-test-renderer'

import Radio from '..'

describe('<Radio />', () => {
  it('should render properly unchecked', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Radio
        option={{
          label: 'Radio',
          value: 'radio',
          legend: 'radioLegend',
          initialValue: false,
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

  it('should render properly checked', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Radio
        option={{
          label: 'Radio',
          value: 'radio',
          legend: 'radioLegend',
          initialValue: true,
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
