import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'
import LogoSvg from '../Icons/logo.svg'

import Input from '../Input'

const WIDTH = 440
const HEIGHT = 580
const LOGO_WIDTH = 143

const Login = ({ onSubmit }) => {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  return (
    <div
      css={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
      }}>
      <form
        css={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-evenly',
          padding: `${spacing.spacious}px ${spacing.colossal}px`,
          width: WIDTH,
          height: HEIGHT,
          backgroundColor: colors.secondaryBackground,
          borderRadius: constants.borderRadius.small,
          boxShadow: constants.boxShadows.default,
        }}>
        <LogoSvg width={LOGO_WIDTH} css={{ margin: '0 auto' }} />
        <h3
          css={{
            textAlign: 'center',
            fontWeight: typography.weight.medium,
            paddingBottom: spacing.spacious,
          }}>
          Welcome. Please login.
        </h3>
        <Input
          id="username"
          label="Username"
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
        <div css={{ padding: spacing.spacious, textAlign: 'center' }}>
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
        <a href="/" css={{ textAlign: 'center' }}>
          Forgot Password? Need login help?
        </a>
      </form>
    </div>
  )
}

Login.propTypes = {
  onSubmit: PropTypes.func.isRequired,
}

export default Login
