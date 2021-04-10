import TestRenderer from 'react-test-renderer'

import organizationUserProjects from '../__mocks__/organizationUserProjects'

import OrganizationUserProjects from '..'

jest.mock('../../Pagination', () => 'Pagination')
jest.mock('../Header', () => 'OrganizationUserProjectsHeader')
jest.mock('../Details', () => 'OrganizationUserProjectsDetails')

const ORGANIZATION_ID = '42869703-fb62-4988-a0d1-e59b15caff06'
const USER_ID = 42

describe('<OrganizationUserProjects />', () => {
  it('should render properly with no projects', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/users/[userId]',
      query: { organizationId: ORGANIZATION_ID, userId: USER_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(<OrganizationUserProjects />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with projects', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/users/[userId]',
      query: { organizationId: ORGANIZATION_ID, userId: USER_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: organizationUserProjects,
    })

    const component = TestRenderer.create(<OrganizationUserProjects />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
