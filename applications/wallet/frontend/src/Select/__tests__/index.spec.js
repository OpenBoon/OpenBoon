import TestRenderer, { act } from 'react-test-renderer'

import Select from '..'

const options = [
  {
    value: '1',
    label: 'a',
  },
  {
    value: '2',
    label: 'b',
  },
]

describe('<Select />', () => {
  it('should handle onChange properly', () => {
    const mockOnChange = jest.fn()

    const component = TestRenderer.create(
      <Select
        label="Test"
        options={options}
        onChange={mockOnChange}
        isRequired
      />,
    )

    act(() => {
      component.root
        .findByType('select')
        .props.onChange({ target: { value: '2' } })
    })

    expect(mockOnChange).toHaveBeenCalledWith({ value: '2' })
  })
})
