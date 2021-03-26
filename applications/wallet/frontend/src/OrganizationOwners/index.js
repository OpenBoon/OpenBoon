import { useRouter } from 'next/router'

import Table from '../Table'

import OrganizationOwnersRow from './Row'

const OrganizationOwners = () => {
  const {
    query: { organizationId },
  } = useRouter()

  return (
    <>
      <Table
        legend="Projects"
        url={`/api/v1/organizations/${organizationId}/owners/`}
        refreshKeys={[]}
        columns={['Owner Email', 'First Name', 'Last Name', '#Actions#']}
        expandColumn={0}
        renderEmpty={<span />}
        renderRow={({ result }) => {
          return <OrganizationOwnersRow key={result.id} owner={result} />
        }}
        refreshButton={false}
      />
    </>
  )
}

export default OrganizationOwners
