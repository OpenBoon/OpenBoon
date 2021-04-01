import TestRenderer from 'react-test-renderer'

import organization from '../__mocks__/organization'

import Organization from '..'

jest.mock('../../OrganizationProjects', () => 'OrganizationProjects')
jest.mock('../../OrganizationUsers', () => 'OrganizationUsers')
jest.mock('../../OrganizationOwners', () => 'OrganizationOwners')
jest.mock('../../OrganizationProjectsAdd', () => 'OrganizationProjectsAdd')

describe('<Organization />', () => {
  it('should render properly for Projects', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]',
      query: {
        organizationId: organization.id,
        action: 'add-project-success',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: organization })

    const component = TestRenderer.create(<Organization />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for Users', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/users',
      query: {
        organizationId: organization.id,
        action: 'delete-project-success',
      },
    })

    require('swr').__setMockUseSWRResponse({ data: organization })

    const component = TestRenderer.create(<Organization />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for Owners', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/owners',
      query: { organizationId: organization.id },
    })

    require('swr').__setMockUseSWRResponse({ data: organization })

    const component = TestRenderer.create(<Organization />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for Create a New Project', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/projects/add',
      query: { organizationId: organization.id },
    })

    require('swr').__setMockUseSWRResponse({ data: organization })

    const component = TestRenderer.create(<Organization />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
