import TestRenderer from 'react-test-renderer'

import organization from '../__mocks__/organization'

import Organization from '..'

// jest.mock('../../OrganizationProjects', () => 'OrganizationProjects')
// jest.mock('../../OrganizationUsers', () => 'OrganizationUsers')
// jest.mock('../../OrganizationOwners', () => 'OrganizationOwners')

const ORGANIZATION_ID = '82d5308b-67c2-1433-8fef-0a580a000956'

describe('<Organization />', () => {
  it('should render properly for Projects', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]',
      query: {
        organizationId: ORGANIZATION_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({ data: organization })

    const component = TestRenderer.create(<Organization />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for Users', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/users',
      query: { organizationId: ORGANIZATION_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: organization })

    const component = TestRenderer.create(<Organization />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for Owners', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/owners',
      query: { organizationId: ORGANIZATION_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: organization })

    const component = TestRenderer.create(<Organization />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
