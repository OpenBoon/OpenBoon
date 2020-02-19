import { useReducer } from 'react'
import Head from 'next/head'
import Link from 'next/link'

import { colors, constants, typography, spacing } from '../Styles'

import LogoSvg from '../Icons/logo.svg'

import FormAlert from '../FormAlert'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import EnterNewPassword from '../EnterNewPassword'

import { onSubmit } from './helpers'

const WIDTH = 446
const LOGO_WIDTH = 143

const INITIAL_STATE = {
  email: '',
  error: '',
}
const reducer = (state, action) => ({ ...state, ...action })

const ResetPassword = () => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  if (action === 'enter-new-password') {
    return <EnterNewPassword />
  }

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

        <h3
          css={{
            textAlign: 'center',
            fontSize: typography.size.large,
            lineHeight: typography.height.large,
            fontWeight: typography.weight.regular,
            paddingTop: spacing.spacious,
            paddingBottom: spacing.spacious,
          }}>
          Did you forget your password?
        </h3>

        <div css={{ color: colors.structure.steel }}>
          Enter your email below and we&apos;ll send you a link to create a new
          one.
        </div>

        <FormAlert
          errorMessage={state.error}
          setErrorMessage={() => dispatch({ error: '' })}
        />

        <Input
          autoFocus
          id="username"
          variant={INPUT_VARIANTS.PRIMARY}
          label="Email"
          type="text"
          value={state.email}
          onChange={({ target: { value } }) => dispatch({ email: value })}
          hasError={!state.email}
        />

        <div
          css={{
            paddingTop: spacing.normal,
            display: 'flex',
            justifyContent: 'center',
          }}>
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => onSubmit({ dispatch, state })}
            isDisabled={!state.email}>
            Request Reset Email
          </Button>
        </div>

        <div
          css={{
            paddingTop: spacing.spacious,
            textAlign: 'center',
            a: {
              color: colors.structure.steel,
            },
          }}>
          <Link href="/">
            <a>Go back to Login</a>
          </Link>
        </div>
      </form>
    </div>
  )
}

export default ResetPassword
