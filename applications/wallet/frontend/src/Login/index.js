import { useState } from 'react'
import PropTypes from 'prop-types'
import Head from 'next/head'

import { colors, constants, typography, spacing } from '../Styles'

import LogoSvg from '../Icons/logo.svg'
import HiddenSvg from '../Icons/hidden.svg'
import VisibleSvg from '../Icons/visible.svg'

import FormAlert from '../FormAlert'
import Input from '../Input'
import Button, { VARIANTS } from '../Button'

import LoginWithGoogle from './WithGoogle'

const WIDTH = 446
const LOGO_WIDTH = 143

const Login = ({
  googleAuth,
  hasGoogleLoaded,
  errorMessage,
  setErrorMessage,
  onSubmit,
}) => {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  return (
    <div
      css={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
      }}>
      <Head>
        <title>Login</title>
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
            fontSize: typography.size.mega,
            lineHeight: typography.height.mega,
            paddingTop: spacing.spacious,
            paddingBottom: spacing.spacious,
          }}>
          Welcome. Please login.
        </h3>

        <LoginWithGoogle
          googleAuth={googleAuth}
          hasGoogleLoaded={hasGoogleLoaded}
          onSubmit={onSubmit}
        />

        <FormAlert
          errorMessage={errorMessage}
          setErrorMessage={setErrorMessage}
        />

        <Input
          autoFocus
          id="username"
          label="Email"
          type="text"
          value={username}
          onChange={({ target: { value } }) => setUsername(value)}
          hasError={!!errorMessage}
        />

        <Input
          id="password"
          label="Password"
          type={showPassword ? 'text' : 'password'}
          value={password}
          onChange={({ target: { value } }) => setPassword(value)}
          hasError={!!errorMessage}
          after={
            <Button
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              variant={VARIANTS.NEUTRAL}
              onClick={() => setShowPassword(!showPassword)}
              style={{
                color: colors.structure.zinc,
                padding: spacing.moderate,
                outlineOffset: -2,
                '&:hover': { color: colors.key.one },
              }}>
              {showPassword ? (
                <VisibleSvg width={20} />
              ) : (
                <HiddenSvg width={20} />
              )}
            </Button>
          }
        />

        <div
          css={{
            paddingTop: spacing.normal,
            display: 'flex',
            justifyContent: 'center',
          }}>
          <Button
            type="submit"
            variant={VARIANTS.PRIMARY}
            onClick={() => onSubmit({ username, password })}
            isDisabled={!username || !password}>
            Login
          </Button>
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
  setErrorMessage: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
}

export default Login
