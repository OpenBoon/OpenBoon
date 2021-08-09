import { useContext } from 'react'
import Head from 'next/head'
import Link from 'next/link'

import { colors, spacing, typography, constants } from '../Styles'

import { UserContext } from '../User'
import Button, { VARIANTS } from '../Button'

import RocketSvg from '../Icons/rocket.svg'

const NoProject = () => {
  const {
    user: { organizations },
  } = useContext(UserContext)

  return (
    <>
      <Head>
        <title>Boon AI</title>
      </Head>

      <div
        css={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: colors.structure.lead,
          marginTop: spacing.spacious,
          boxShadow: constants.boxShadows.tableRow,
        }}
      >
        <RocketSvg height={210} />

        <h2
          css={{
            color: colors.key.two,
            fontSize: typography.size.enormous,
            lineHeight: typography.height.enormous,
            paddingTop: spacing.base,
          }}
        >
          Welcome to Boon AI!
        </h2>

        {organizations.length === 0 && (
          <>
            <h3
              css={{
                fontSize: typography.size.colossal,
                lineHeight: typography.height.colossal,
                fontWeight: typography.weight.regular,
                paddingTop: spacing.normal,
              }}
            >
              To get started you will need to be added to a project.
            </h3>

            <div
              css={{
                display: 'flex',
                padding: spacing.base,
                color: colors.structure.zinc,
                fontSize: typography.size.large,
                lineHeight: typography.height.large,
              }}
            >
              Request project access from your administrator.
            </div>
          </>
        )}

        {organizations.length === 1 && (
          <>
            <h3
              css={{
                fontSize: typography.size.colossal,
                lineHeight: typography.height.colossal,
                fontWeight: typography.weight.regular,
              }}
            >
              Create a new project to get started.
            </h3>

            <div css={{ display: 'flex', padding: spacing.spacious }}>
              <Link
                href={`/organizations/${organizations[0]}/projects/add`}
                passHref
              >
                <Button variant={VARIANTS.PRIMARY}>Create a New Project</Button>
              </Link>
            </div>
          </>
        )}

        {organizations.length > 1 && (
          <>
            <h3
              css={{
                fontSize: typography.size.colossal,
                lineHeight: typography.height.colossal,
                fontWeight: typography.weight.regular,
              }}
            >
              To get started select your organization and create a new project.
            </h3>

            <div css={{ display: 'flex', padding: spacing.spacious }}>
              <Link href="/organizations" passHref>
                <Button variant={VARIANTS.PRIMARY}>Select Organization</Button>
              </Link>
            </div>
          </>
        )}
      </div>
    </>
  )
}

export default NoProject
