import { useRouter } from 'next/router'

import SuspenseBoundary from '../SuspenseBoundary'
import Table from '../Table'

import OrganizationUserProjectsHeader from './Header'
import OrganizationUserProjectsDetails from './Details'
import OrganizationUserProjectsRow from './Row'

const OrganizationUserProjects = () => {
  const {
    query: { organizationId, userId },
  } = useRouter()

  return (
    <SuspenseBoundary>
      <OrganizationUserProjectsHeader />

      <OrganizationUserProjectsDetails />

      <Table
        legend="User Projects"
        url={`/api/v1/organizations/${organizationId}/users/${userId}/projects/`}
        refreshKeys={[]}
        columns={['Project Name', 'Roles', '#Actions#']}
        expandColumn={2}
        renderEmpty={<span />}
        renderRow={({ result, revalidate }) => {
          return (
            <OrganizationUserProjectsRow
              key={result.id}
              userId={parseInt(userId, 10)}
              project={result}
              revalidate={revalidate}
            />
          )
        }}
        refreshButton={false}
      />
    </SuspenseBoundary>
  )
}

export default OrganizationUserProjects
