import TestRenderer from 'react-test-renderer'

import organizationOwners from '../__mocks__/organizationOwners'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import OrganizationOwners from '..'

jest.mock('../../Pagination', () => 'Pagination')

const ORGANIZATION_ID = '42869703-fb62-4988-a0d1-e59b15caff06'

describe('<OrganizationOwners />', () => {
  it('should render properly with no owners', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/owners',
      query: { organizationId: ORGANIZATION_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(
      <User initialUser={{ ...mockUser, email: 'software@zorroa.com' }}>
        <OrganizationOwners />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with owners', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/owners',
      query: { organizationId: ORGANIZATION_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: organizationOwners,
    })

    const component = TestRenderer.create(
      <User initialUser={{ ...mockUser, email: 'software@zorroa.com' }}>
        <OrganizationOwners />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should hide the menu gear for the active user', () => {
    require('swr').__setMockUseSWRResponse({
      data: organizationOwners,
    })

    const component = TestRenderer.create(
      <User initialUser={{ ...mockUser, email: 'software@zorroa.com' }}>
        <OrganizationOwners />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
