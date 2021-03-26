import { useRouter } from 'next/router'

import SuspenseBoundary from '../SuspenseBoundary'
import Tabs from '../Tabs'

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

      {pathname === '/organizations/[organizationId]' && 'Project Usage'}

      {pathname === '/organizations/[organizationId]/users' && 'Users'}

      {pathname === '/organizations/[organizationId]/owners' && 'Owners'}
    </SuspenseBoundary>
  )
}

export default Organization
