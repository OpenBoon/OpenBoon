import { useRouter } from 'next/router'

import SuspenseBoundary from '../SuspenseBoundary'
import Tabs from '../Tabs'
import OrganizationOwners from '../OrganizationOwners'
import OrganizationProjects from '../OrganizationProjects'
import OrganizationUsers from '../OrganizationUsers'

import OrganizationDetails from './Details'

const Organization = () => {
  const { pathname } = useRouter()

  return (
    <SuspenseBoundary>
      <OrganizationDetails key={pathname} />

      <Tabs
        tabs={[
          { title: 'Project Usage', href: '/organizations/[organizationId]' },
          { title: 'Users', href: '/organizations/[organizationId]/users' },
          { title: 'Owners', href: '/organizations/[organizationId]/owners' },
        ]}
      />

      {pathname === '/organizations/[organizationId]' && (
        <OrganizationProjects />
      )}

      {pathname === '/organizations/[organizationId]/users' && (
        <OrganizationUsers />
      )}

      {pathname === '/organizations/[organizationId]/owners' && (
        <OrganizationOwners />
      )}
    </SuspenseBoundary>
  )
}

export default Organization
