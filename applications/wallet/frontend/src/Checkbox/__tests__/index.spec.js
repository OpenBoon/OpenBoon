import TestRenderer, { act } from 'react-test-renderer'

import Checkbox, { VARIANTS } from '..'
import { VARIANTS as CHECKBOX_ICON_VARIANTS } from '../Icon'

describe('<Checkbox />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Checkbox
        value="checkbox"
        label="Checkbox"
        icon=""
        legend=""
        onClick={mockFn}
        initialValue={false}
        variant={VARIANTS.PRIMARY}
        iconVariant={CHECKBOX_ICON_VARIANTS.PRIMARY}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ type: 'checkbox' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockFn).toHaveBeenCalledWith(true)
  })
})
