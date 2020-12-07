import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import GeneratingSvg from '../Icons/generating.svg'
import GoogleLogoSvg from '../Icons/google_logo.svg'

const WIDTH = 200
const HEIGHT = 20

const GOOGLE_COLORS = {
  default: '#318AF5',
  hover: '#0053C4',
}

const LoginWithGoogle = ({ googleAuth, hasGoogleLoaded, onSubmit }) => {
  const [isLoading, setIsLoading] = useState(false)

  return (
    <div
      css={{
        paddingBottom: spacing.comfy,
        display: 'flex',
        justifyContent: 'center',
      }}
    >
      <Button
        aria-label="Sign in with Google"
        variant={VARIANTS.NEUTRAL}
        isDisabled={!hasGoogleLoaded || isLoading}
        style={{
          alignItems: 'stretch',
          width: WIDTH,
          minWidth: WIDTH,
          maxWidth: WIDTH,
          backgroundColor: GOOGLE_COLORS.default,
          padding: spacing.mini,
          ':hover': { backgroundColor: GOOGLE_COLORS.hover },
          '&[aria-disabled=true]': {
            opacity: constants.opacity.half,
            color: colors.structure.white,
            backgroundColor: GOOGLE_COLORS.default,
          },
        }}
        onClick={async () => {
          setIsLoading(!isLoading)

          try {
            const googleUser = await googleAuth.signIn()

            const { id_token: idToken } = googleUser.getAuthResponse()

            await onSubmit({ idToken })

            setIsLoading(false)
          } catch (error) {
            setIsLoading(false)
          }
        }}
      >
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <div
            css={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: spacing.moderate,
              backgroundColor: isLoading
                ? colors.structure.transparent
                : colors.structure.white,
              borderRadius: constants.borderRadius.small,
            }}
          >
            {isLoading ? (
              <GeneratingSvg
                height={HEIGHT}
                css={{ animation: constants.animations.infiniteRotation }}
              />
            ) : (
              <GoogleLogoSvg height={HEIGHT} />
            )}
          </div>

          <div css={{ flex: isLoading ? 0 : 1 }}>
            {isLoading ? 'Signing In' : 'Sign in with Google'}
          </div>
        </div>
      </Button>
    </div>
  )
}

LoginWithGoogle.propTypes = {
  googleAuth: PropTypes.shape({
    signIn: PropTypes.func.isRequired,
  }).isRequired,
  hasGoogleLoaded: PropTypes.bool.isRequired,
  onSubmit: PropTypes.func.isRequired,
}

export default LoginWithGoogle
