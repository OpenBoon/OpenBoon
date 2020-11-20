import TestRenderer, { act } from 'react-test-renderer'

import Combobox from '..'
import ComboboxContainer from '../Container'

describe('Combobox', () => {
  it('should render properly with a options function', async () => {
    const mockFn = jest.fn()
    const options = [{ label: 'label0', count: 1 }]
    const value = options[0].label

    const component = TestRenderer.create(
      <Combobox
        label="inputLabel"
        options={async () => Promise.resolve(options)}
        value={value}
        onChange={mockFn}
        hasError={false}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // First action needs to be async to catch the changes made by useEffect
    await act(async () => {
      component.root
        .findByProps({ 'data-reach-combobox-input': '' })
        .props.onChange({ target: { value: 'Jane' } })
    })

    expect(
      component.root.findByProps({ 'data-reach-combobox-input': '' }).props
        .value,
    ).toEqual(value)

    act(() => {
      component.root.findByType(ComboboxContainer).props.onSelect('Jane')
    })

    expect(mockFn).toHaveBeenCalledTimes(2)
    expect(mockFn).toHaveBeenLastCalledWith({ value: 'Jane' })
  })

  it('should render properly with an options array', () => {
    const mockFn = jest.fn()
    const options = [{ label: 'label0', count: 1 }]
    const value = options[0].label

    const component = TestRenderer.create(
      <Combobox
        label="inputLabel"
        options={options}
        value={value}
        onChange={mockFn}
        hasError={false}
        errorMessage=""
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with error', () => {
    const mockFn = jest.fn()
    const options = [{ label: 'label0', count: 1 }]
    const value = options[0].label

    const component = TestRenderer.create(
      <Combobox
        label="inputLabel"
        options={options}
        value={value}
        onChange={mockFn}
        hasError
        errorMessage="error"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should unmount properly with an options function', () => {
    const mockFn = jest.fn()
    const options = [{ label: 'label0', count: 1 }]
    const value = options[0].label

    const component = TestRenderer.create(
      <Combobox
        label="inputLabel"
        options={async () => Promise.resolve(options)}
        value={value}
        onChange={mockFn}
        hasError={false}
      />,
    )

    component.unmount()
  })
})
