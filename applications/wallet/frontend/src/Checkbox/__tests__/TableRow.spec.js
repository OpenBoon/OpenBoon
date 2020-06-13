import TestRenderer, { act } from 'react-test-renderer'

import CheckboxTableRow from '../TableRow'

const noop = () => () => {}

describe('<CheckboxTableRow />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <CheckboxTableRow
        option={{
          value: 'api',
          label: 'API',
          icon: '',
          legend: "Dude You're Getting A Telescope",
          initialValue: true,
          isDisabled: true,
        }}
        onClick={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ type: 'checkbox' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockFn).not.toHaveBeenCalled()
  })
})
