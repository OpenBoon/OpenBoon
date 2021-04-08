import TestRenderer from 'react-test-renderer'

import user from '../__mocks__/user'

import OrganizationUserProjectsDetails from '../Details'

const ORGANIZATION_ID = '42869703-fb62-4988-a0d1-e59b15caff06'
const USER_ID = 42

describe('<OrganizationUserProjectsDetails />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/users/[userId]',
      query: { organizationId: ORGANIZATION_ID, userId: USER_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: user })

    const component = TestRenderer.create(<OrganizationUserProjectsDetails />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
