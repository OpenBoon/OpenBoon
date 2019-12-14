import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'

import WarningSvg from '../Icons/warning.svg'
import CrossSvg from '../Icons/cross.svg'

const SPACING = spacing.moderate
const ICON_HEIGHT = 20

const FormAlert = ({ errorMessage }) => {
  const [displayError, setDisplayError] = useState(errorMessage)

  if (!errorMessage || !displayError) {
    return (
      <div css={{ padding: SPACING }}>
        <div css={{ padding: SPACING }}>
          <div css={{ height: ICON_HEIGHT }} />
        </div>
      </div>
    )
  }

  return (
    <div css={{ paddingTop: SPACING, paddingBottom: SPACING }}>
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          backgroundColor: colors.signal.warning.background,
          borderRadius: constants.borderRadius.small,
          padding: SPACING,
        }}>
        <WarningSvg height={ICON_HEIGHT} color={colors.warning} />

        <div
          role="alert"
          css={{
            flex: 1,
            paddingLeft: SPACING,
            color: colors.structure.black,
            fontWeight: typography.weight.medium,
          }}>
          {errorMessage}
        </div>

        <button
          type="button"
          aria-label="Close alert"
          css={{ border: 0, padding: 0, background: 'none', display: 'flex' }}
          onClick={() => setDisplayError(false)}>
          <CrossSvg height={ICON_HEIGHT} color={colors.structure.black} />
        </button>
      </div>
    </div>
  )
}

FormAlert.propTypes = {
  errorMessage: PropTypes.string.isRequired,
}

export default FormAlert
