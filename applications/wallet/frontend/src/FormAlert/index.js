import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'

import WarningSvg from '../Icons/warning.svg'
import CrossSvg from '../Icons/cross.svg'

const PADDING_OUTER = spacing.base
const PADDING_INNER = spacing.moderate
const ICON_HEIGHT = 20

const FormAlert = ({ setErrorMessage, children }) => {
  if (!children) {
    return (
      <div css={{ padding: PADDING_OUTER }}>
        <div css={{ padding: PADDING_INNER }}>
          <div css={{ height: ICON_HEIGHT }} />
        </div>
      </div>
    )
  }

  return (
    <div css={{ paddingTop: PADDING_OUTER, paddingBottom: PADDING_OUTER }}>
      <div
        css={{
          display: 'flex',
          alignItems: setErrorMessage ? 'center' : 'flex-start',
          backgroundColor: colors.signal.warning.background,
          borderRadius: constants.borderRadius.small,
          padding: PADDING_INNER,
        }}>
        <WarningSvg height={ICON_HEIGHT} color={colors.signal.warning.base} />

        <div
          role="alert"
          css={{
            flex: 1,
            paddingLeft: PADDING_INNER,
            color: colors.structure.black,
            fontWeight: typography.weight.medium,
          }}>
          {children}
        </div>

        {setErrorMessage && (
          <button
            type="button"
            aria-label="Close alert"
            css={{ border: 0, padding: 0, background: 'none', display: 'flex' }}
            onClick={() => setErrorMessage('')}>
            <CrossSvg height={ICON_HEIGHT} color={colors.structure.black} />
          </button>
        )}
      </div>
    </div>
  )
}

FormAlert.propTypes = {
  setErrorMessage: PropTypes.oneOfType([PropTypes.bool, PropTypes.func])
    .isRequired,
  children: PropTypes.node.isRequired,
}

export default FormAlert
