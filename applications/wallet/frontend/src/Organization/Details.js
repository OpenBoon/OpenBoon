import Head from 'next/head'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import Breadcrumbs from '../Breadcrumbs'

import { getTitle } from './helpers'

const OrganizationDetails = () => {
  const {
    pathname,
    query: { organizationId },
  } = useRouter()

  const { data: organization } = useSWR(
    `/api/v1/organizations/${organizationId}/`,
  )

  return (
    <>
      <Head>
        <title>
          {organization.name} {getTitle({ pathname })}
        </title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Organization Admin', href: '/organizations' },
          { title: organization.name, href: false },
        ]}
      />
    </>
  )
}

export default OrganizationDetails
