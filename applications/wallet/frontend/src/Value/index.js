import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

const STYLES = {
  PRIMARY: {
    container: {
      display: 'flex',
      flexDirection: 'column',
      paddingLeft: spacing.comfy,
      paddingRight: spacing.comfy,
    },
    legend: {
      color: colors.structure.iron,
    },
  },
  SECONDARY: {
    legend: {
      color: colors.structure.white,
      fontWeight: typography.weight.bold,
    },
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Value = ({ variant, legend, children }) => (
  <div css={STYLES[variant].container}>
    <div css={{ ...STYLES[variant].legend, paddingBottom: spacing.small }}>
      {legend}:
    </div>
    <div css={{ color: colors.structure.steel }}>{children}</div>
  </div>
)

Value.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  legend: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default Value
