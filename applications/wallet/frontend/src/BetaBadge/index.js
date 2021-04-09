import { colors, constants, spacing, typography } from '../Styles'

const LETTER_SPACING = 0.2

const BetaBadge = () => {
  return (
    <span
      css={{
        color: colors.structure.white,
        backgroundColor: colors.signal.halloween.base,
        borderRadius: constants.borderRadius.round,
        fontSize: typography.size.invisible,
        lineHeight: typography.height.invisible,
        fontWeight: typography.weight.medium,
        textTransform: 'uppercase',
        letterSpacing: LETTER_SPACING,
        padding: spacing.hairline,
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
