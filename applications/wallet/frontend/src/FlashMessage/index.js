import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'
import WarningSvg from '../Icons/warning.svg'
import InformationSvg from '../Icons/information.svg'
import GeneratingSvg from '../Icons/generating.svg'

const PADDING = spacing.moderate

const STYLES = {
  SUCCESS: {
    backgroundColor: colors.signal.grass.background,
    icon: (
      <CheckmarkSvg
        height={constants.icons.regular}
        color={colors.signal.grass.base}
      />
    ),
    linkBackground: colors.signal.grass.base,
    linkHover: colors.signal.grass.strong,
  },
  ERROR: {
    backgroundColor: colors.signal.warning.background,
    icon: (
      <WarningSvg
        height={constants.icons.regular}
        color={colors.signal.warning.base}
      />
    ),
    linkBackground: colors.structure.smoke,
    linkHover: colors.structure.mattGrey,
  },
  INFO: {
    backgroundColor: colors.signal.sky.background,
    icon: (
      <InformationSvg
        height={constants.icons.regular}
        color={colors.signal.sky.base}
      />
    ),
    linkBackground: colors.signal.sky.base,
    linkHover: colors.signal.sky.strong,
  },
  PROCESSING: {
    backgroundColor: colors.signal.sky.background,
    icon: (
      <GeneratingSvg
        height={constants.icons.regular}
        color={colors.signal.sky.base}
        css={{ animation: constants.animations.infiniteRotation }}
      />
    ),
    linkBackground: colors.signal.sky.base,
    linkHover: colors.signal.sky.strong,
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
          whiteSpace: 'pre-line',
          a: {
            color: colors.structure.white,
            backgroundColor: STYLES[variant].linkBackground,
            padding: spacing.small + spacing.mini,
            marginLeft: spacing.base,
            paddingLeft: spacing.base,
            paddingRight: spacing.base,
            borderRadius: constants.borderRadius.small,
            ':hover': {
              textDecoration: 'none',
              backgroundColor: STYLES[variant].linkHover,
            },
          },
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
