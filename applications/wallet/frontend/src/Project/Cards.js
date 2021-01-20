import useSWR from 'swr'
import { useRouter } from 'next/router'

import { colors, spacing, constants, typography } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'
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
    data: { id, name },
  } = useSWR(`/api/v1/projects/${projectId}/`)

  return (
    <div
      css={{
        display: 'flex',
        gap: spacing.comfy,
        flexWrap: 'wrap',
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
            borderRadius: constants.borderRadius.small,
            padding: spacing.comfy,
          }}
        >
          <h3 css={{ paddingBottom: spacing.base }}>Project: {name}</h3>

          <div
            css={{
              color: colors.structure.zinc,
              paddingBottom: spacing.normal,
            }}
          >
            Project ID: {id}
          </div>

          <SuspenseBoundary isTransparent>
            <ProjectMetrics projectId={projectId} />
          </SuspenseBoundary>

          <div
            css={{
              fontSize: typography.size.small,
              lineHeight: typography.height.small,
              color: colors.structure.zinc,
              paddingTop: spacing.comfy,
              paddingBottom: spacing.base,
            }}
          >
            *pages are processed &amp; counted as individual assets
          </div>
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
