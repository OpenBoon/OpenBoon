import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'
import Table from '../Table'

import OrganizationProjectsRow from './Row'

const OrganizationProjects = () => {
  const {
    query: { organizationId },
  } = useRouter()

  return (
    <>
      <div
        css={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          paddingBottom: spacing.normal,
        }}
      >
        <div>Total Module Usage &amp; Storage / Current Billing Cycle:</div>

        <Link href={`/organizations/${organizationId}/projects/add`} passHref>
          <Button variant={VARIANTS.PRIMARY_SMALL}>Create a New Project</Button>
        </Link>
      </div>

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
              organizationId={organizationId}
              project={result}
              revalidate={revalidate}
            />
          )
        }}
        refreshButton={false}
      />
    </>
  )
}

export default OrganizationProjects
