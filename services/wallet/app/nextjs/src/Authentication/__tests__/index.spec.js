import TestRenderer, { act } from 'react-test-renderer'

import Authentication from '../'

jest.mock('../../Login', () => 'Login')

describe('<Authentication />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Authentication>{({ user }) => `Hello ${user.email}!`}</Authentication>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Login').props.onSubmit({ email: 'World' })()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
