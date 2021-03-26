import { useRouter } from 'next/router'

import Table from '../Table'

import OrganizationUsersRow from './Row'

const OrganizationUsers = () => {
  const {
    query: { organizationId },
  } = useRouter()

  return (
    <>
      <Table
        legend="Users"
        url={`/api/v1/organizations/${organizationId}/users/`}
        refreshKeys={[]}
        columns={[
          'User Email',
          'First Name',
          'Last Name',
          'Projects',
          '#Actions#',
        ]}
        expandColumn={0}
        renderEmpty={<span />}
        renderRow={({ result }) => {
          return <OrganizationUsersRow key={result.id} user={result} />
        }}
        refreshButton={false}
      />
    </>
  )
}

export default OrganizationUsers
