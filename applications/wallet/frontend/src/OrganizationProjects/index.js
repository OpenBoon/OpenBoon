import { useRouter } from 'next/router'

import Table from '../Table'

import OrganizationProjectsRow from './Row'

const OrganizationProjects = () => {
  const {
    query: { organizationId },
  } = useRouter()

  return (
    <Table
      legend="Projects"
      url={`/api/v1/organizations/${organizationId}/projects/`}
      refreshKeys={[]}
      columns={[
        'Project Name',
        'Images & Docs* \n Internal Modules \n ML Usage',
        'Images & Docs* \n External Modules \n ML Usage',
        'Images & Docs* \n Total Assets \n Stored',
        'Video \n Internal Modules \n ML Usage',
        'Video \n External Modules \n ML Usage',
        'Video \n Total Hours \n Stored',
        '#Actions#',
      ]}
      expandColumn={0}
      renderEmpty={<span />}
      renderRow={({ result, revalidate }) => {
        return (
          <OrganizationProjectsRow
            key={result.id}
            project={result}
            revalidate={revalidate}
          />
        )
      }}
      refreshButton={false}
    />
  )
}

export default OrganizationProjects
