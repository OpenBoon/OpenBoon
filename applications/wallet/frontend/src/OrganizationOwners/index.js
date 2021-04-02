import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'
import Table from '../Table'

import OrganizationOwnersRow from './Row'

const OrganizationOwners = () => {
  const {
    query: { organizationId },
  } = useRouter()

  return (
    <>
      <div
        css={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'flex-end',
          justifyContent: 'flex-end',
          paddingBottom: spacing.normal,
        }}
      >
        <Link href={`/organizations/${organizationId}/owners/add`} passHref>
          <Button variant={VARIANTS.PRIMARY_SMALL}>
            Add an Organization Owner
          </Button>
        </Link>
      </div>

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
    </>
  )
}

export default OrganizationOwners
