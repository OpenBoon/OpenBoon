import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import NoProject from '..'

describe('<NoProject />', () => {
  it('should render properly with no organization', async () => {
    const component = TestRenderer.create(
      <User initialUser={{ ...mockUser, organizations: [] }}>
        <NoProject />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with one organization', async () => {
    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <NoProject />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with more than one organization', async () => {
    const component = TestRenderer.create(
      <User initialUser={{ ...mockUser, organizations: ['one', 'two'] }}>
        <NoProject />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
