import TestRenderer from 'react-test-renderer'

import organizationUsers from '../__mocks__/organizationUsers'

import OrganizationUsers from '..'

jest.mock('../../Pagination', () => 'Pagination')

const ORGANIZATION_ID = '42869703-fb62-4988-a0d1-e59b15caff06'

describe('<OrganizationUsers />', () => {
  it('should render properly with no users', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/users',
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

    const component = TestRenderer.create(<OrganizationUsers />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with users', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/users',
      query: { organizationId: ORGANIZATION_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: organizationUsers,
    })

    const component = TestRenderer.create(<OrganizationUsers />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
