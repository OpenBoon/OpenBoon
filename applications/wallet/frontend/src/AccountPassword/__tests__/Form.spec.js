import TestRenderer, { act } from 'react-test-renderer'

import AccountPasswordForm from '../Form'

const noop = () => () => {}

describe('<AccountPasswordForm />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <AccountPasswordForm onSubmit={mockFn} />,
    )

    act(() => {
      component.root
        .findByProps({ id: 'currentPassword' })
        .props.onChange({ target: { value: 'foo' } })
    })

    act(() => {
      component.root
        .findByProps({ id: 'newPassword' })
        .props.onChange({ target: { value: 'bar' } })
    })

    act(() => {
      component.root
        .findByProps({ id: 'confirmPassword' })
        .props.onChange({ target: { value: 'bar' } })
    })

    act(() => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalledWith({
      currentPassword: 'foo',
      newPassword: 'bar',
      confirmPassword: 'bar',
    })
  })
})
