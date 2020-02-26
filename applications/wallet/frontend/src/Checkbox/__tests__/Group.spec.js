import TestRenderer, { act } from 'react-test-renderer'

import { VARIANTS as CHECKBOX_VARIANTS } from '..'
import CheckboxGroup from '../Group'

describe('<CheckboxGroup />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <CheckboxGroup
        variant={CHECKBOX_VARIANTS.PRIMARY}
        legend="Permissions"
        onClick={mockFn}
        options={[
          {
            value: 'api',
            label: 'API',
            icon: '',
            legend: "Dude You're Getting A Telescope",
            initialValue: true,
            isDisabled: false,
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
