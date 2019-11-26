import { colors, constants, typography, spacing } from '../Styles'

import Input from '../Input'

import LogoSvg from './logo.svg'

const WIDTH = 440
const HEIGHT = 580
const LOGO_WIDTH = 143

const Login = () => (
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
      <Input id="email" label="Email" type="text" value="" />
      <Input id="password" label="Password" type="password" value="" />
      <a href="/" css={{ textAlign: 'center' }}>
        Forgot Password? Need login help?
      </a>
    </form>
  </div>
)

export default Login
