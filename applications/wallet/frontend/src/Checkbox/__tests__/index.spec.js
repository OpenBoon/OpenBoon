import TestRenderer, { act } from 'react-test-renderer'

import Checkbox from '..'

describe('<Checkbox />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Checkbox
        label="Checkbox"
        legend=""
        onClick={mockFn}
        initialValue={false}
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
