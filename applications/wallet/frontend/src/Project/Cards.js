import useSWR from 'swr'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { colors, spacing, constants } from '../Styles'

import Button, { VARIANTS } from '../Button'
import Bouncer, { ROLES } from '../Bouncer'

import ProjectGettingStarted from './GettingStarted'
import ProjectQuickLinks from './QuickLinks'

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
          <Bouncer role={ROLES.User_Admin}>
            <div css={{ display: 'flex' }}>
              <Link
                href="/[projectId]/users/add"
                as={`/${projectId}/users/add`}
                passHref
              >
                <Button variant={VARIANTS.PRIMARY_SMALL}>
                  + Add Users To Project
                </Button>
              </Link>
            </div>
          </Bouncer>
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
