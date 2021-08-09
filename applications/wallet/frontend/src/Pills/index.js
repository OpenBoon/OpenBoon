import { colors, spacing, constants, typography } from '../Styles'

const Pills = ({ children }) => {
  return children.map((child) => (
    <span
      key={child}
      css={{
        display: 'inline-block',
        color: colors.structure.coal,
        backgroundColor: colors.structure.zinc,
        padding: spacing.moderate,
        paddingTop: spacing.small,
        paddingBottom: spacing.small,
        marginRight: spacing.base,
        borderRadius: constants.borderRadius.large,
        fontFamily: typography.family.condensed,
      }}
    >
      {child.includes('_')
        ? child.replaceAll('_', ' ')
        : child.replace(/([A-Z])/g, (match) => ` ${match}`)}
    </span>
  ))
}

export default Pills
