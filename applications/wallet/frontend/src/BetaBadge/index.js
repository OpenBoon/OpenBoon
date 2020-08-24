import { colors, spacing, typography } from '../Styles'

const BORDER_RADIUS = 8
const FONT_SIZE = 11
const LETTER_SPACING = 0.2

const BetaBadge = () => {
  return (
    <span
      css={{
        color: colors.structure.white,
        backgroundColor: colors.signal.sky.base,
        borderRadius: BORDER_RADIUS,
        fontSize: FONT_SIZE,
        lineHeight: `${FONT_SIZE * 1.4}px`,
        fontWeight: typography.weight.medium,
        textTransform: 'uppercase',
        letterSpacing: LETTER_SPACING,
        padding: spacing.mini,
        paddingLeft: spacing.mini * 3,
        paddingRight: spacing.mini * 3,
        marginLeft: spacing.base,
      }}
    >
      beta
    </span>
  )
}

export default BetaBadge
