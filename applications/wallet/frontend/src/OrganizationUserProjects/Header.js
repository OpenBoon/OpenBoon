import Head from 'next/head'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import Breadcrumbs from '../Breadcrumbs'

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
          { title: 'User Projects', href: false },
        ]}
      />
    </>
  )
}

export default OrganizationUserProjectsHeader
