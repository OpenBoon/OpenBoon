import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

const STYLES = {
  PRIMARY: {
    container: {
      display: 'flex',
      flexDirection: 'column',
      paddingRight: spacing.giant,
      paddingTop: spacing.moderate,
      paddingBottom: spacing.moderate,
    },
  },
  SECONDARY: {
    container: {
      paddingTop: spacing.normal,
    },
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Value = ({ variant, legend, children }) => (
  <div css={STYLES[variant].container}>
    <div
      css={{
        fontWeight: typography.weight.bold,
        color: colors.structure.white,
        paddingBottom: spacing.small,
      }}
    >
      {legend}:
    </div>
    <div css={{ color: colors.structure.zinc }}>{children}</div>
  </div>
)

Value.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  legend: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default Value
