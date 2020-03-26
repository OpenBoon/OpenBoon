import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Bouncer, { ROLES } from '..'

describe('<Bouncer />', () => {
  it('should render the child properly with the appropriate role', () => {
    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Bouncer role={ROLES.ML_Tools}>Please come in!</Bouncer>
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not render the child with no roles', () => {
    const component = TestRenderer.create(
      <User initialUser={{ ...mockUser, roles: undefined }}>
        <Bouncer role={ROLES.ML_Tools}>No shorts or open shoes!</Bouncer>
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
