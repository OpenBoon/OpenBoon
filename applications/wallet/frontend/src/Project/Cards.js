import useSWR from 'swr'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { colors, spacing } from '../Styles'

import Card from '../Card'
import Button, { VARIANTS } from '../Button'

import KeySvg from '../Icons/key.svg'

import ProjectUsagePlan from './UsagePlan'
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
      }}>
      <Card title="">
        <h3>Project: {name}</h3>
        &nbsp;
        <div>Project ID: {id}</div>
        &nbsp;
        <div css={{ display: 'flex' }}>
          <Link
            href="/[projectId]/users/add"
            as={`/${projectId}/users/add`}
            passHref>
            <Button variant={VARIANTS.PRIMARY_SMALL}>
              + Add Users To Project
            </Button>
          </Link>
        </div>
      </Card>

      <ProjectUsagePlan />

      <ProjectGettingStarted projectId={projectId} />

      <Card
        title={
          <>
            <KeySvg width={20} aria-hidden color={colors.structure.zinc} />
            Project API Keys
          </>
        }>
        <p
          css={{
            margin: 0,
            paddingBottom: spacing.normal,
            color: colors.structure.zinc,
          }}>
          Create a ZMLP API key for use with external applications and tools.
        </p>
        <div css={{ display: 'flex' }}>
          <Link
            href="/[projectId]/api-keys/add"
            as={`/${projectId}/api-keys/add`}
            passHref>
            <Button variant={VARIANTS.SECONDARY_SMALL}>
              + Create an API Key
            </Button>
          </Link>
        </div>
      </Card>
    </div>
  )
}

export default ProjectCards
