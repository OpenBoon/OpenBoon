import useSWR from 'swr'
import {useRouter} from 'next/router'
import Link from 'next/link'

import {colors, constants, spacing} from '../Styles'

import Card, {VARIANTS as CARD_VARIANTS} from '../Card'
import Button, {VARIANTS} from '../Button'
import Bouncer, {ROLES} from '../Bouncer'

import KeySvg from '../Icons/key.svg'

import ProjectGettingStarted from './GettingStarted'

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
        flexDirection: 'column',
        flexWrap: 'wrap',
        maxHeight: '100vh',
        alignContent: 'flex-start',
      }}
    >
      <Card
        variant={CARD_VARIANTS.LIGHT}
        header=""
        content={
          <>
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
          </>
        }
      />

      <Bouncer role={ROLES.ML_Tools}>
        <ProjectGettingStarted projectId={projectId} />
      </Bouncer>

      <Bouncer role={ROLES.API_Keys}>
        <Card
          variant={CARD_VARIANTS.LIGHT}
          header={
            <h3 css={{ display: 'flex' }}>
              <KeySvg
                height={constants.icons.regular}
                color={colors.structure.zinc}
              />
              Project API Keys
            </h3>
          }
          content={
            <>
              <p
                css={{
                  margin: 0,
                  paddingBottom: spacing.normal,
                  color: colors.structure.zinc,
                }}
              >
                Create a ZMLP API key for use with external applications and
                tools.
              </p>
              <div css={{ display: 'flex' }}>
                <Link
                  href="/[projectId]/api-keys/add"
                  as={`/${projectId}/api-keys/add`}
                  passHref
                >
                  <Button variant={VARIANTS.SECONDARY_SMALL}>
                    + Create API Key
                  </Button>
                </Link>
              </div>
            </>
          }
        />
      </Bouncer>
    </div>
  )
}

export default ProjectCards
