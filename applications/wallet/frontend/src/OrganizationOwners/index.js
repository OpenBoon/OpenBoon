import { useRouter } from 'next/router'

import Table from '../Table'

import OrganizationOwnersRow from './Row'

const OrganizationOwners = () => {
  const {
    query: { organizationId },
  } = useRouter()

  return (
    <Table
      legend="Owners"
      url={`/api/v1/organizations/${organizationId}/owners/`}
      refreshKeys={[]}
      columns={['Owner Email', 'First Name', 'Last Name', '#Actions#']}
      expandColumn={0}
      renderEmpty={<span />}
      renderRow={({ result, revalidate }) => {
        return (
          <OrganizationOwnersRow
            key={result.id}
            organizationId={organizationId}
            owner={result}
            revalidate={revalidate}
          />
        )
      }}
      refreshButton={false}
    />
  )
}

export default OrganizationOwners
