import TestRenderer, { act } from 'react-test-renderer'

import user from '../__mocks__/user'

import User, { UserContext, noop } from '..'

describe('<User />', () => {
  it('should update localStorage and the user state', () => {
    const component = TestRenderer.create(
      <User initialUser={user}>
        <UserContext.Consumer>
          {({ user: { email }, setUser }) => (
            <div>
              <span>{email}</span>
              <button
                type="button"
                onClick={() => setUser({ user: email ? null : user })}>
                {email ? 'Logout' : 'Login'}
              </button>
            </div>
          )}
        </UserContext.Consumer>
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Logout' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(component.root.findByType('span').props.children).toBeUndefined()

    act(() => {
      component.root
        .findByProps({ children: 'Login' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(component.root.findByType('span').props.children).toBe(user.email)
  })

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
