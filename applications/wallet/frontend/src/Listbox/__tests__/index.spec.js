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
        inputLabel="Filter types"
        options={options}
        onChange={mockOnChange}
        value=""
        placeholder="Select Type"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // search non-existent value
    act(() => {
      component.root
        .findByProps({ type: 'search' })
        .props.onChange({ target: { value: 'qwerty' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // search existing value
    act(() => {
      component.root
        .findByProps({ type: 'search' })
        .props.onChange({ target: { value: 'time' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('ListboxInput').props.onChange('clip.timeline')
    })

    expect(component.toJSON()).toMatchSnapshot()
    expect(mockOnChange).toBeCalledWith({ value: 'clip.timeline' })
  })

  it('should render properly with existing attribute', () => {
    const mockOnChange = jest.fn()

    const component = TestRenderer.create(
      <Listbox
        label="Metadata Type"
        inputLabel="Filter types"
        options={options}
        onChange={mockOnChange}
        value="clip.timeline"
        placeholder="timeline"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
