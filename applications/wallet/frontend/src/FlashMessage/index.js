import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'
import WarningSvg from '../Icons/warning.svg'

const PADDING_OUTER = spacing.base
const PADDING_INNER = spacing.moderate
const ICON_HEIGHT = 20

const STYLES = {
  SUCCESS: {
    border: constants.borders.success,
    backgroundColor: colors.signal.grass.background,
    icon: (
      <CheckmarkSvg height={ICON_HEIGHT} color={colors.signal.grass.base} />
    ),
  },
  ERROR: {
    border: constants.borders.error,
    backgroundColor: colors.signal.warning.background,
    icon: (
      <WarningSvg height={ICON_HEIGHT} color={colors.signal.warning.base} />
    ),
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const FlashMessage = ({ variant, children }) => {
  return (
    <div
      css={{
        display: 'flex',
        paddingTop: PADDING_OUTER,
        paddingBottom: PADDING_OUTER,
      }}
    >
      <div
        css={{
          display: 'flex',
          alignItems: 'flex-start',
          backgroundColor: STYLES[variant].backgroundColor,
          borderRadius: constants.borderRadius.small,
          padding: PADDING_INNER,
        }}
      >
        {STYLES[variant].icon}

        <div
          role="alert"
          css={{
            flex: 1,
            paddingLeft: PADDING_INNER,
            color: colors.structure.black,
            fontWeight: typography.weight.medium,
          }}
        >
          {children}
        </div>
      </div>
    </div>
  )
}

FlashMessage.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  children: PropTypes.node.isRequired,
}

export default FlashMessage
