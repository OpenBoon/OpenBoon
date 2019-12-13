import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'
import LogoSvg from '../Icons/logo.svg'

import Input from '../Input'

const WIDTH = 440
const LOGO_WIDTH = 143

const Login = ({ onSubmit }) => {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  return (
    <div
      css={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
      }}>
      <form
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
          css={{ alignSelf: 'center', paddingTop: spacing.comfy }}
        />
        <h3
          css={{
            textAlign: 'center',
            fontSize: typography.size.mega,
            lineHeight: typography.height.mega,
            margin: 0,
            paddingTop: spacing.spacious,
            paddingBottom: spacing.colossal,
          }}>
          Welcome. Please login.
        </h3>
        <Input
          id="username"
          label="Email"
          type="text"
          value={username}
          onChange={({ target: { value } }) => setUsername(value)}
        />
        <Input
          id="password"
          label="Password"
          type="password"
          value={password}
          onChange={({ target: { value } }) => setPassword(value)}
        />
        <div css={{ padding: spacing.comfy, textAlign: 'center' }}>
          <button
            type="submit"
            onClick={event => {
              event.preventDefault()
              onSubmit({ username, password })
            }}
            css={{
              backgroundColor: colors.primary,
              color: colors.primaryFont,
              fontSize: typography.size.hecto,
              lineHeight: typography.height.hecto,
              fontWeight: typography.weight.medium,
              borderRadius: constants.borderRadius.small,
              padding: `${spacing.moderate}px ${spacing.spacious}px`,
              border: 0,
              cursor: 'pointer',
              ':hover': {
                backgroundColor: colors.primaryHover,
              },
            }}>
            Login
          </button>
        </div>
      </form>
    </div>
  )
}

Login.propTypes = {
  onSubmit: PropTypes.func.isRequired,
}

export default Login
