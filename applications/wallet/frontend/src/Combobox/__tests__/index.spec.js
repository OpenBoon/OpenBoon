import TestRenderer, { act } from 'react-test-renderer'

import Combobox from '..'
import ComboboxContainer from '../Container'

describe('Combobox', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()
    const options = [{ label: 'label0', count: 1 }]
    const originalValue = options[0].label

    const component = TestRenderer.create(
      <Combobox
        id="comboboxId"
        inputLabel="inputLabel"
        options={options}
        originalValue={originalValue}
        currentValue="currentValue"
        onChange={mockFn}
        hasError={false}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'data-reach-combobox-input': '' })
        .props.onChange({ target: { value: 'Jane' } })
    })

    expect(mockFn).toHaveBeenCalledTimes(1)
    expect(mockFn).toHaveBeenCalledWith({ value: 'Jane' })

    act(() => {
      component.root
        .findByProps({ 'data-reach-combobox-input': '' })
        .props.onBlur({ target: { value: 'Jane' } })
    })

    expect(mockFn).toHaveBeenCalledTimes(2)

    act(() => {
      component.root
        .findByProps({ 'data-reach-combobox-input': '' })
        .props.onBlur({ target: { value: '' } })
    })

    expect(mockFn).toHaveBeenCalledTimes(3)
    expect(mockFn).toHaveBeenCalledWith({ value: originalValue })

    act(() => {
      component.root.findByType(ComboboxContainer).props.onSelect('')
    })

    expect(mockFn).toHaveBeenCalledTimes(4)
    expect(mockFn).toHaveBeenLastCalledWith({ value: '' })
  })

  it('should render properly with error', () => {
    const mockFn = jest.fn()
    const options = [{ label: 'label0', count: 1 }]
    const originalValue = options[0].label

    const component = TestRenderer.create(
      <Combobox
        id="comboboxId"
        inputLabel="inputLabel"
        options={options}
        originalValue={originalValue}
        currentValue="currentValue"
        onChange={mockFn}
        hasError
        errorMessage="error"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
