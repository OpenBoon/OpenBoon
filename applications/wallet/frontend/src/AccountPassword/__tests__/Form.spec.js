import TestRenderer, { act } from 'react-test-renderer'

import AccountPasswordForm from '../Form'

const noop = () => () => {}

describe('<AccountPasswordForm />', () => {
  it('should render properly', async () => {
    const component = TestRenderer.create(<AccountPasswordForm />)

    expect(component.toJSON()).toMatchSnapshot()

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

    fetch.mockResponseOnce(
      JSON.stringify({
        oldPassword: 'password',
        newPassword1: 'password1',
        newPassword2: 'password1',
      }),
    )

    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Click edit password again
    act(() => {
      component.root
        .findByProps({ children: 'Edit Password Again' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when a user cancels', () => {
    const component = TestRenderer.create(<AccountPasswordForm />)

    // Click cancel
    act(() => {
      component.root
        .findByProps({ children: 'Cancel' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
