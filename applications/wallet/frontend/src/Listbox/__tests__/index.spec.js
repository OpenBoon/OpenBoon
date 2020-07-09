import TestRenderer, { act } from 'react-test-renderer'

import options from '../__mocks__/options'

import Listbox from '..'

jest.mock('../Options', () => 'ListboxOptions')

describe('Listbox', () => {
  it('should render properly', () => {
    const mockOnChange = jest.fn()

    const component = TestRenderer.create(
      <Listbox
        label="Metadata Type"
        options={options}
        onChange={mockOnChange}
        value=""
        placeholder="Select Type"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByType('ListboxInput')
        .props.onChange('system.timeCreated')
    })

    expect(component.toJSON()).toMatchSnapshot()
    expect(mockOnChange).toBeCalledWith({ value: 'system.timeCreated' })
  })

  it('should render properly with existing attribute', () => {
    const mockOnChange = jest.fn()

    const component = TestRenderer.create(
      <Listbox
        label="Metadata Type"
        options={options}
        onChange={mockOnChange}
        value="system.timeCreated"
        placeholder="timeCreated"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
