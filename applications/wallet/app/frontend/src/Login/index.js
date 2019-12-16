import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'

import LogoSvg from '../Icons/logo.svg'
import HiddenSvg from '../Icons/hidden.svg'
import VisibleSvg from '../Icons/visible.svg'

import FormAlert from '../FormAlert'
import Input from '../Input'
import Button, { VARIANTS } from '../Button'

const WIDTH = 440
const LOGO_WIDTH = 143

const Login = ({ errorMessage, setErrorMessage, onSubmit }) => {
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
      <form
        method="post"
        onSubmit={event => event.preventDefault()}
        css={{
          display: 'flex',
          flexDirection: 'column',
          padding: `${spacing.normal}px ${spacing.colossal}px`,
          width: WIDTH,
          backgroundColor: colors.secondaryBackground,
          borderRadius: constants.borderRadius.small,
          boxShadow: constants.boxShadows.default,
        }}>
        <LogoSvg
          width={LOGO_WIDTH}
          css={{
            alignSelf: 'center',
            paddingTop: spacing.comfy,
            paddingBottom: spacing.comfy,
          }}
        />
        <h3
          css={{
            textAlign: 'center',
            fontSize: typography.size.mega,
            lineHeight: typography.height.mega,
            margin: 0,
            paddingTop: spacing.moderate,
            paddingBottom: spacing.moderate,
          }}>
          Welcome. Please login.
        </h3>
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
              style={{ '&:hover': { color: colors.primary } }}>
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
            padding: spacing.comfy,
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
  errorMessage: PropTypes.string.isRequired,
  setErrorMessage: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
}

export default Login
