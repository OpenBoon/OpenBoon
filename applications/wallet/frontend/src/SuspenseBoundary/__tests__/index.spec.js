import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import SuspenseBoundary, { ROLES } from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<SuspenseBoundary />', () => {
  it('should render the child properly with the appropriate role', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <SuspenseBoundary role={ROLES.ML_Tools}>
          Please come in!
        </SuspenseBoundary>
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not render the child with no roles', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={{ ...mockUser, roles: undefined }}>
        <SuspenseBoundary role={ROLES.ML_Tools}>
          No shorts or open shoes!
        </SuspenseBoundary>
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
