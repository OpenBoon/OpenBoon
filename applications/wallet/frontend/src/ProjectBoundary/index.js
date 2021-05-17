import { useContext } from 'react'
import Head from 'next/head'
import Link from 'next/link'

import { spacing, colors, typography, constants } from '../Styles'

import { logout } from '../Authentication/helpers'

import { UserContext } from '../User'
import Layout from '../Layout'
import Button, { VARIANTS } from '../Button'

import LockSvg from '../Icons/lock.svg'

const ProjectBoundary = () => {
  const { user, googleAuth } = useContext(UserContext)

  return (
    <Layout user={{ ...user, projectId: '' }} logout={logout({ googleAuth })}>
      <Head>
        <title>Access Denied</title>
      </Head>

      <div css={{ height: '100%' }}>
        <div
          css={{
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            backgroundColor: colors.structure.lead,
            boxShadow: constants.boxShadows.default,
          }}
        >
          <LockSvg width={150} color={colors.structure.steel} />

          <h3
            css={{
              paddingTop: spacing.normal,
              fontSize: typography.size.colossal,
              lineHeight: typography.height.colossal,
              fontWeight: typography.weight.bold,
              maxWidth: constants.paragraph.maxWidth,
            }}
          >
            You do not have permission
            <br /> to access this project.
          </h3>

          <p
            css={{
              fontSize: typography.size.large,
              lineHeight: typography.height.large,
              color: colors.structure.zinc,
              maxWidth: constants.paragraph.maxWidth,
            }}
          >
            Please request permission from the project owner.
          </p>

          <div css={{ paddingTop: spacing.normal }}>
            <Link href="/" passHref>
              <Button variant={VARIANTS.PRIMARY}>Go to Dashboard</Button>
            </Link>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default ProjectBoundary
