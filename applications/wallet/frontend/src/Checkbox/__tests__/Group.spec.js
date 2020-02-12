import TestRenderer, { act } from 'react-test-renderer'

import { VARIANTS as CHECKBOX_VARIANTS } from '..'
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
            icon: '',
            legend: "Dude You're Getting A Telescope",
            initialValue: true,
          },
        ]}
        variant={CHECKBOX_VARIANTS.PRIMARY}
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
