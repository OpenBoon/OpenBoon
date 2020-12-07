import { useState } from 'react'
import PropTypes from 'prop-types'
import Head from 'next/head'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, constants, typography, spacing } from '../Styles'

import LargeLogo from '../Icons/largeLogo.svg'
import HiddenSvg from '../Icons/hidden.svg'
import VisibleSvg from '../Icons/visible.svg'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import LoginWithGoogle from './WithGoogle'

const WIDTH = 446
const LOGO_WIDTH = 180

const Login = ({ googleAuth, hasGoogleLoaded, errorMessage, onSubmit }) => {
  const {
    query: { action },
  } = useRouter()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
      }}
    >
      <Head>
        <title>Login</title>
      </Head>

      {action === 'password-reset-request-success' && (
        <div css={{ paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Password reset email sent.
          </FlashMessage>
        </div>
      )}

      {action === 'password-reset-update-success' && (
        <div css={{ paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Password has been updated.
          </FlashMessage>
        </div>
      )}

      {action === 'account-activation-success' && (
        <div css={{ paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Account activated. Please login now.
          </FlashMessage>
        </div>
      )}

      <form
        method="post"
        onSubmit={(event) => event.preventDefault()}
        css={{
          display: 'flex',
          flexDirection: 'column',
          padding: spacing.colossal,
          width: WIDTH,
          backgroundColor: colors.structure.mattGrey,
          borderRadius: constants.borderRadius.small,
          boxShadow: constants.boxShadows.default,
        }}
      >
        <LargeLogo width={LOGO_WIDTH} css={{ alignSelf: 'center' }} />

        <h3
          css={{
            textAlign: 'center',
            fontSize: typography.size.large,
            lineHeight: typography.height.large,
            paddingTop: spacing.spacious,
            paddingBottom: spacing.spacious,
          }}
        >
          Welcome. Please login.
        </h3>

        <LoginWithGoogle
          googleAuth={googleAuth}
          hasGoogleLoaded={hasGoogleLoaded}
          onSubmit={onSubmit}
        />

        {!!errorMessage && (
          <div
            css={{
              paddingBottom: spacing.base,
            }}
          >
            <FlashMessage variant={FLASH_VARIANTS.ERROR}>
              {errorMessage}
              <br />
              <a
                href="mailto:support@zorroa.com?subject=ZVI Console Login Trouble"
                target="_blank"
                rel="noopener noreferrer"
              >
                Contact Support
              </a>
            </FlashMessage>
          </div>
        )}

        <Input
          autoFocus
          id="username"
          variant={INPUT_VARIANTS.PRIMARY}
          label="Email"
          type="text"
          value={username}
          onChange={({ target: { value } }) => setUsername(value)}
          hasError={!!errorMessage}
        />

        <Input
          id="password"
          variant={INPUT_VARIANTS.PRIMARY}
          label="Password"
          type={showPassword ? 'text' : 'password'}
          value={password}
          onChange={({ target: { value } }) => setPassword(value)}
          hasError={!!errorMessage}
          after={
            <Button
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              variant={BUTTON_VARIANTS.NEUTRAL}
              onClick={() => setShowPassword(!showPassword)}
              style={{
                color: colors.structure.zinc,
                padding: spacing.moderate,
                outlineOffset: -2,
                '&:hover': { color: colors.key.one },
              }}
            >
              {showPassword ? (
                <VisibleSvg height={constants.icons.regular} />
              ) : (
                <HiddenSvg height={constants.icons.regular} />
              )}
            </Button>
          }
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
            onClick={() => onSubmit({ username, password })}
            isDisabled={!username || !password}
          >
            Login
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
          <Link href="/create-account">
            <a>Create an account</a>
          </Link>
          <br />
          <br />
          <Link href="/reset-password">
            <a>Reset your password</a>
          </Link>
        </div>
      </form>
    </div>
  )
}

Login.propTypes = {
  googleAuth: PropTypes.shape({
    signIn: PropTypes.func.isRequired,
  }).isRequired,
  hasGoogleLoaded: PropTypes.bool.isRequired,
  errorMessage: PropTypes.string.isRequired,
  onSubmit: PropTypes.func.isRequired,
}

export default Login
