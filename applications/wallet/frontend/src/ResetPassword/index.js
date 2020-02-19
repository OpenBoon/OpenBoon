import Head from 'next/head'
import { useRouter } from 'next/router'

import { colors, constants, spacing } from '../Styles'

import LogoSvg from '../Icons/logo.svg'

import ResetPasswordConfirm from './Confirm'
import ResetPasswordRequest from './Request'

const WIDTH = 446
const LOGO_WIDTH = 143

const ResetPassword = () => {
  const {
    query: { action, uid, token },
  } = useRouter()

  return (
    <div
      css={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
      }}>
      <Head>
        <title>Reset Password</title>
      </Head>

      <form
        method="post"
        onSubmit={event => event.preventDefault()}
        css={{
          display: 'flex',
          flexDirection: 'column',
          padding: spacing.colossal,
          width: WIDTH,
          backgroundColor: colors.structure.mattGrey,
          borderRadius: constants.borderRadius.small,
          boxShadow: constants.boxShadows.default,
        }}>
        <LogoSvg width={LOGO_WIDTH} css={{ alignSelf: 'center' }} />

        {uid && token ? (
          <ResetPasswordConfirm uid={uid} token={token} />
        ) : (
          <ResetPasswordRequest action={action} />
        )}
      </form>
    </div>
  )
}

export default ResetPassword
