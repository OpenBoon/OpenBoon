import TestRenderer, { act } from 'react-test-renderer'

import Select from '..'

const options = [
  {
    key: '1',
    value: 'a',
  },
  {
    key: '2',
    value: 'b',
  },
]

describe('<Select />', () => {
  it('should handle onChange properly', () => {
    const mockOnChange = jest.fn()

    const component = TestRenderer.create(
      <Select
        name="Test"
        label="Test:"
        options={options}
        onChange={mockOnChange}
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
