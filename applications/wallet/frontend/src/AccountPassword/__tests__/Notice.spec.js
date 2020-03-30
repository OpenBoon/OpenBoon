import TestRenderer, { act } from 'react-test-renderer'

import AccountPasswordForm from '../Form'

const noop = () => () => {}

describe('<AccountPasswordNotice />', () => {
  it('should render an error', async () => {
    const component = TestRenderer.create(<AccountPasswordForm />)

    // Mock Failure
    fetch.mockRejectOnce({ error: 'Invalid' }, { status: 400 })

    await act(async () => {
      component.root
        .findByProps({ children: 'Reset Password' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
