import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing, typography, colors } from '../Styles'

import Button, { VARIANTS } from '../Button'
import Table from '../Table'

import { getCurrentPeriod } from './helpers'

import OrganizationProjectsRow from './Row'
import OrganizationProjectsAggregate from './Aggregate'

const OrganizationProjects = () => {
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
          justifyContent: 'space-between',
        }}
      >
        <div
          css={{
            fontWeight: typography.weight.medium,
            fontSize: typography.size.medium,
            lineHeight: typography.height.medium,
            paddingBottom: spacing.normal,
          }}
        >
          Total Module Usage &amp; Storage / Current Billing Cycle:{' '}
          {getCurrentPeriod({ date: new Date() })}
        </div>

        <div css={{ paddingBottom: spacing.normal }}>
          <Link href={`/organizations/${organizationId}/projects/add`} passHref>
            <Button variant={VARIANTS.PRIMARY_SMALL}>
              Create a New Project
            </Button>
          </Link>
        </div>
      </div>

      <OrganizationProjectsAggregate />

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

      <div
        css={{
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          color: colors.structure.zinc,
          paddingBottom: spacing.base,
          fontFamily: typography.family.condensed,
        }}
      >
        *pages are processed &amp; counted as individual assets
        <br />
        **usage is being calculated and is currently unavailable
      </div>
    </>
  )
}

export default OrganizationProjects
