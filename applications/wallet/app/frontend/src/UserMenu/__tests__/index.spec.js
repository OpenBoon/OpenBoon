import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import UserMenu from '..'

const noop = () => () => {}

describe('<UserMenu />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <UserMenu user={mockUser} logout={noop} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Jane Doe' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
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
