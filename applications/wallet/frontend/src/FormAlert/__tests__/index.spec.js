import TestRenderer, { act } from 'react-test-renderer'

import FormAlert from '..'

describe('<FormAlert />', () => {
  it('should render properly with an alert', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <FormAlert setErrorMessage={mockFn}>Error Message</FormAlert>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close alert' })
        .props.onClick()
    })

    expect(mockFn).toHaveBeenCalledWith('')
  })
})
