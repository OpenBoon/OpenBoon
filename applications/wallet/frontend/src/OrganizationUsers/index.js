import { useRouter } from 'next/router'

import Table from '../Table'

import OrganizationUsersRow from './Row'

const OrganizationUsers = () => {
  const {
    query: { organizationId },
  } = useRouter()

  return (
    <Table
      legend="Users"
      url={`/api/v1/organizations/${organizationId}/users/`}
      refreshKeys={[]}
      columns={[
        'User Email',
        'First Name',
        'Last Name',
        'Projects',
        '#Actions#',
      ]}
      expandColumn={0}
      renderEmpty="There are currently no users in this organization."
      renderRow={({ result, revalidate }) => {
        return (
          <OrganizationUsersRow
            key={result.id}
            organizationId={organizationId}
            user={result}
            revalidate={revalidate}
          />
        )
      }}
      refreshButton={false}
    />
  )
}

export default OrganizationUsers
