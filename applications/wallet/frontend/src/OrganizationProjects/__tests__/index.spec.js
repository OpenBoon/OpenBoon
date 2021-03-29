import TestRenderer from 'react-test-renderer'

import organizationProjects from '../__mocks__/organizationProjects'

import OrganizationProjects from '..'

jest.mock('../../Pagination', () => 'Pagination')

const ORGANIZATION_ID = '42869703-fb62-4988-a0d1-e59b15caff06'

describe('<OrganizationProjects />', () => {
  it('should render properly with no projects', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]',
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

    const component = TestRenderer.create(<OrganizationProjects />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with projects', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]',
      query: { organizationId: ORGANIZATION_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: organizationProjects,
    })

    const component = TestRenderer.create(<OrganizationProjects />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
