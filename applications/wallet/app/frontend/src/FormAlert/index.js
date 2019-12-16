import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'

import WarningSvg from '../Icons/warning.svg'
import CrossSvg from '../Icons/cross.svg'

const PADDING = spacing.moderate
const ICON_HEIGHT = 20

const FormAlert = ({ errorMessage, setErrorMessage }) => {
  if (!errorMessage) {
    return (
      <div css={{ padding: PADDING }}>
        <div css={{ padding: PADDING }}>
          <div css={{ height: ICON_HEIGHT }} />
        </div>
      </div>
    )
  }

  return (
    <div css={{ paddingTop: PADDING, paddingBottom: PADDING }}>
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          backgroundColor: colors.signal.warning.background,
          borderRadius: constants.borderRadius.small,
          padding: PADDING,
        }}>
        <WarningSvg height={ICON_HEIGHT} color={colors.warning} />

        <div
          role="alert"
          css={{
            flex: 1,
            paddingLeft: PADDING,
            color: colors.structure.black,
            fontWeight: typography.weight.medium,
          }}>
          {errorMessage}
        </div>

        <button
          type="button"
          aria-label="Close alert"
          css={{ border: 0, padding: 0, background: 'none', display: 'flex' }}
          onClick={() => setErrorMessage('')}>
          <CrossSvg height={ICON_HEIGHT} color={colors.structure.black} />
        </button>
      </div>
    </div>
  )
}

FormAlert.propTypes = {
  errorMessage: PropTypes.string.isRequired,
  setErrorMessage: PropTypes.func.isRequired,
}

export default FormAlert
