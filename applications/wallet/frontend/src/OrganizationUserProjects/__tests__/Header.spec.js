import TestRenderer from 'react-test-renderer'

import organization from '../../Organization/__mocks__/organization'

import OrganizationUserProjectsHeader from '../Header'

const ORGANIZATION_ID = '42869703-fb62-4988-a0d1-e59b15caff06'
const USER_ID = 42

describe('<OrganizationUserProjectsHeader />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/users/[userId]',
      query: { organizationId: ORGANIZATION_ID, userId: USER_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: organization })

    const component = TestRenderer.create(<OrganizationUserProjectsHeader />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
