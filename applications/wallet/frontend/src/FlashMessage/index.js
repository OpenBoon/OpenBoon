import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'
import WarningSvg from '../Icons/warning.svg'

const SIZE = 40

const STYLES = {
  SUCCESS: {
    border: constants.borders.success,
    backgroundColor: colors.signal.grass.base,
    icon: <CheckmarkSvg width={SIZE / 2} />,
  },
  ERROR: {
    border: constants.borders.error,
    backgroundColor: colors.signal.warning.base,
    icon: <WarningSvg width={SIZE / 2} />,
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
        paddingTop: spacing.base,
        paddingBottom: spacing.base,
      }}
    >
      <div
        css={{
          display: 'flex',
          justifySelf: 'flex-start',
          color: colors.structure.black,
          fontWeight: typography.weight.medium,
          backgroundColor: colors.structure.white,
          border: STYLES[variant].border,
          boxShadow: constants.boxShadows.default,
        }}
      >
        <div
          css={{
            width: SIZE,
            height: SIZE,
            color: colors.structure.white,
            backgroundColor: STYLES[variant].backgroundColor,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {STYLES[variant].icon}
        </div>
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            paddingLeft: spacing.normal,
            paddingRight: spacing.normal,
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
  children: PropTypes.string.isRequired,
}

export default FlashMessage
