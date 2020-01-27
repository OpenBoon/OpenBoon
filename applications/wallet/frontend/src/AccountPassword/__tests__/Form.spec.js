import TestRenderer, { act } from 'react-test-renderer'

import AccountPasswordForm from '../Form'

const noop = () => () => {}

jest.mock('../helpers')

describe('<AccountPasswordForm />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<AccountPasswordForm />)

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

    expect(component.toJSON()).toMatchSnapshot()
    // expect(mockFn).toHaveBeenCalledWith({
    //   currentPassword: 'foo',
    //   newPassword: 'bar',
    //   confirmPassword: 'bar',
    // })
  })
})
