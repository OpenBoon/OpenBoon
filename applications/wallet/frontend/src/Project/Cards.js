import useSWR from 'swr'
import { useRouter } from 'next/router'

import { colors, spacing, constants, typography } from '../Styles'

import Bouncer, { ROLES } from '../Bouncer'

import ProjectMetrics from './Metrics'
import ProjectQuickLinks from './QuickLinks'
import ProjectGettingStarted from './GettingStarted'

const MAX_WIDTH = 880

const ProjectCards = () => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: { id, name, organizationName, mlUsageThisMonth, totalStorageUsage },
  } = useSWR(`/api/v1/projects/${projectId}/`)

  return (
    <div
      css={{
        display: 'flex',
        gap: spacing.comfy,
        flexWrap: 'wrap',
        paddingTop: spacing.normal,
        paddingBottom: spacing.spacious,
      }}
    >
      <div
        css={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          gap: spacing.comfy,
          maxWidth: MAX_WIDTH,
        }}
      >
        <div
          css={{
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: colors.structure.smoke,
            boxShadow: constants.boxShadows.tableRow,
            borderRadius: constants.borderRadius.medium,
            padding: spacing.comfy,
          }}
        >
          <h3
            css={{
              paddingBottom: spacing.base,
              fontSize: typography.size.giant,
              lineHeight: typography.height.giant,
            }}
          >
            {name}
          </h3>

          <div
            css={{
              color: colors.structure.zinc,
              paddingBottom: spacing.spacious,
            }}
          >
            Organization: {organizationName} / Project ID:&nbsp;
            <span css={{ textTransform: 'uppercase' }}>{id}</span>
          </div>

          <ProjectMetrics
            mlUsageThisMonth={mlUsageThisMonth}
            totalStorageUsage={totalStorageUsage}
          />
        </div>

        <ProjectQuickLinks projectId={projectId} />
      </div>

      <Bouncer role={ROLES.ML_Tools}>
        <ProjectGettingStarted projectId={projectId} />
      </Bouncer>
    </div>
  )
}

export default ProjectCards
