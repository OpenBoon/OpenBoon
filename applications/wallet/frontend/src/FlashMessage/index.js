import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'
import WarningSvg from '../Icons/warning.svg'
import GeneratingSvg from '../Icons/generating.svg'

const PADDING = spacing.moderate
const ICON_SIZE = 20

const STYLES = {
  SUCCESS: {
    border: constants.borders.success,
    backgroundColor: colors.signal.grass.background,
    icon: <CheckmarkSvg width={ICON_SIZE} color={colors.signal.grass.base} />,
  },
  ERROR: {
    border: constants.borders.error,
    backgroundColor: colors.signal.warning.background,
    icon: <WarningSvg width={ICON_SIZE} color={colors.signal.warning.base} />,
  },
  PROCESSING: {
    border: constants.borders.error,
    backgroundColor: colors.signal.sky.background,
    icon: <GeneratingSvg width={ICON_SIZE} color={colors.signal.sky.base} />,
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
        alignItems: 'flex-start',
        backgroundColor: STYLES[variant].backgroundColor,
        borderRadius: constants.borderRadius.small,
        padding: PADDING,
      }}
    >
      {STYLES[variant].icon}

      <div
        role="alert"
        css={{
          flex: 1,
          paddingLeft: PADDING,
          color: colors.structure.coal,
          fontWeight: typography.weight.medium,
        }}
      >
        {children}
      </div>
    </div>
  )
}

FlashMessage.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  children: PropTypes.node.isRequired,
}

export default FlashMessage
