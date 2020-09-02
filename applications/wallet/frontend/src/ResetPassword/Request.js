import { useReducer } from 'react'
import Link from 'next/link'

import { colors, typography, spacing } from '../Styles'

import FlashMessageErrors from '../FlashMessage/Errors'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { onRequest } from './helpers'

const INITIAL_STATE = {
  email: '',
  error: '',
}
const reducer = (state, action) => ({ ...state, ...action })

const ResetPasswordRequest = () => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <>
      <h3
        css={{
          textAlign: 'center',
          fontSize: typography.size.large,
          lineHeight: typography.height.large,
          fontWeight: typography.weight.regular,
          paddingTop: spacing.spacious,
          paddingBottom: spacing.spacious,
        }}
      >
        Did you forget your password?
      </h3>

      <div css={{ color: colors.structure.zinc }}>
        Enter your email below and we&apos;ll send you a link to create a new
        one.
      </div>

      <FlashMessageErrors
        errors={{ global: state.error }}
        styles={{ paddingTop: spacing.comfy, paddingBottom: spacing.normal }}
      />

      <Input
        autoFocus
        id="email"
        variant={INPUT_VARIANTS.PRIMARY}
        label="Email"
        type="text"
        value={state.email}
        onChange={({ target: { value } }) => dispatch({ email: value })}
        hasError={false}
      />

      <div
        css={{
          paddingTop: spacing.normal,
          display: 'flex',
          justifyContent: 'center',
        }}
      >
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onRequest({ dispatch, state })}
          isDisabled={!state.email}
        >
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
        }}
      >
        <Link href="/">
          <a>Go back to Login</a>
        </Link>
      </div>
    </>
  )
}

export default ResetPasswordRequest
