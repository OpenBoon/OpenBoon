import Head from 'next/head'

import { colors, constants, spacing, typography } from '../Styles'

import Navbar from '../Navbar'

import EnvelopeSvg from '../Icons/envelope.svg'

export const noop = () => () => {}

const CreateAccountSuccess = () => {
  return (
    <div css={{ height: '100%' }}>
      <Head>
        <title>Create an Account</title>
      </Head>

      <Navbar projectId="" isSidebarOpen={false} setSidebarOpen={noop}>
        <div />
      </Navbar>

      <div
        css={{
          marginTop: constants.navbar.height,
          padding: spacing.spacious,
          height: `calc(100vh - ${constants.navbar.height}px)`,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <div
          css={{
            backgroundColor: colors.structure.white,
            padding: spacing.enormous,
            paddingLeft: spacing.enormous + spacing.comfy,
            paddingRight: spacing.enormous + spacing.comfy,
            textAlign: 'center',
            maxWidth: '50%',
            borderRadius: constants.borderRadius.small,
          }}
        >
          <h2 css={{ color: colors.key.one }}>Confirmation email sent!</h2>

          <EnvelopeSvg
            width={142}
            css={{ marginTop: spacing.large, marginBottom: spacing.comfy }}
          />

          <p
            css={{
              color: colors.structure.coal,
              fontWeight: typography.weight.bold,
              fontSize: typography.size.large,
              lineHeight: typography.height.large,
            }}
          >
            Please click the confirmation link sent to your email
            <br />
            to activate your account.
          </p>

          <p
            css={{
              color: colors.structure.steel,
              fontWeight: typography.weight.medium,
            }}
          >
            Don’t see it? Check your spam just in case. :)
          </p>
        </div>
      </div>
    </div>
  )
}

export default CreateAccountSuccess
