import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import OrganizationOwners from '../OrganizationOwners'
import OrganizationProjects from '../OrganizationProjects'
import OrganizationUsers from '../OrganizationUsers'
import OrganizationProjectsAdd from '../OrganizationProjectsAdd'

import OrganizationDetails from './Details'

const Organization = () => {
  const {
    pathname,
    query: { action },
  } = useRouter()

  return (
    <SuspenseBoundary>
      <OrganizationDetails key={pathname} />

      {!!action && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Project {action === 'add-project-success' && 'created'}
            {action === 'delete-project-success' && 'deleted'}.
          </FlashMessage>
        </div>
      )}

      <Tabs
        tabs={[
          { title: 'Project Usage', href: '/organizations/[organizationId]' },
          { title: 'Users', href: '/organizations/[organizationId]/users' },
          {
            title: 'Owners',
            href: '/organizations/[organizationId]/owners',
          },
          pathname === '/organizations/[organizationId]/projects/add'
            ? {
                title: 'Create Project',
                href: '/organizations/[organizationId]/projects/add',
                isSelected: true,
              }
            : {},
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

      {pathname === '/organizations/[organizationId]/projects/add' && (
        <OrganizationProjectsAdd />
      )}
    </SuspenseBoundary>
  )
}

export default Organization
