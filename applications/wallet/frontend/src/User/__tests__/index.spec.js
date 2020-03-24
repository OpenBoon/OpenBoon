import TestRenderer, { act } from 'react-test-renderer'

import user from '../__mocks__/user'

import User, { UserContext, noop } from '..'

import { USER } from '../helpers'

describe('<User />', () => {
  it('should update localStorage and the user state', () => {
    const mockRemoveItem = jest.fn()
    const mockSetItem = jest.fn()

    Object.defineProperty(window, 'localStorage', {
      writable: true,
      value: {
        removeItem: mockRemoveItem,
        setItem: mockSetItem,
      },
    })

    const component = TestRenderer.create(
      <User initialUser={user}>
        <UserContext.Consumer>
          {({ user: { email }, setUser }) => (
            <div>
              <span>{email}</span>
              <button
                type="button"
                onClick={() => setUser({ user: email ? null : user })}
              >
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

    expect(mockRemoveItem).toHaveBeenCalledWith(USER)

    act(() => {
      component.root
        .findByProps({ children: 'Login' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(component.root.findByType('span').props.children).toBe(user.email)

    expect(mockSetItem).toHaveBeenCalledWith(USER, JSON.stringify(user))
  })

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
