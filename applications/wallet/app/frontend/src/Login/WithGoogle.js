import PropTypes from 'prop-types'

import Button, { VARIANTS } from '../Button'

const LoginWithGoogle = ({ googleAuth, hasGoogleLoaded, onSubmit }) => {
  return (
    <>
      <Button
        variant={VARIANTS.PRIMARY}
        isDisabled={!hasGoogleLoaded}
        onClick={async () => {
          const response = await googleAuth.signIn()

          const { id_token: idToken } = response?.Zi || {}

          onSubmit({ idToken })
        }}>
        Sign In with Google
      </Button>
    </>
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
