import Head from 'next/head'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import Breadcrumbs from '../Breadcrumbs'
import OrganizationBadge from '../Organization/Badge'

const OrganizationUserProjectsHeader = () => {
  const {
    query: { organizationId },
  } = useRouter()

  const { data: organization } = useSWR(
    `/api/v1/organizations/${organizationId}/`,
  )

  return (
    <>
      <Head>
        <title>{organization.name} User Projects</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Organization Admin', href: '/organizations' },
          {
            title: organization.name,
            href: `/organizations/${organizationId}`,
          },
          {
            title: 'Users',
            href: `/organizations/${organizationId}/users`,
          },
          { title: 'Projects', href: false },
        ]}
      />

      <OrganizationBadge>{organization.plan}</OrganizationBadge>
    </>
  )
}

export default OrganizationUserProjectsHeader
