import TestRenderer, { act } from 'react-test-renderer'

import CheckboxGroup from '../Group'

describe('<CheckboxGroup />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <CheckboxGroup
        legend="Permissions"
        onClick={mockFn}
        options={[
          {
            key: 'api',
            label: 'API',
            legend: "Dude You're Getting A Telescope",
            initialValue: true,
          },
        ]}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ type: 'checkbox' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockFn).toHaveBeenCalledWith({ api: false })
  })
})
