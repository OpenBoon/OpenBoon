import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import UserMenu from '..'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<UserMenu />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <UserMenu user={mockUser} logout={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open user menu' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Contact Support' })
        .props.onClick({ preventDefault: noop })
    })
  })

  it('should logout user', () => {
    const mockLogout = jest.fn()
    const component = TestRenderer.create(
      <UserMenu user={mockUser} logout={mockLogout} />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open user menu' })
        .props.onClick({ preventDefault: noop })
    })

    act(() => {
      component.root
        .findByProps({ children: 'Sign Out' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockLogout).toHaveBeenCalledWith({
      redirectUrl: '/',
      redirectAs: '/',
    })
  })

  it('should render properly without firstName/lastName', () => {
    const component = TestRenderer.create(
      <UserMenu
        user={{ ...mockUser, firstName: '', lastName: '' }}
        logout={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
