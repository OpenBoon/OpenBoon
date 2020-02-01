import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

const Value = ({ legend, children }) => (
  <div
    css={{
      display: 'flex',
      flexDirection: 'column',
      paddingLeft: spacing.comfy,
      paddingRight: spacing.comfy,
    }}>
    <div css={{ color: colors.structure.iron, paddingBottom: spacing.small }}>
      {legend}:
    </div>
    <div css={{ color: colors.structure.steel }}>{children}</div>
  </div>
)

Value.propTypes = {
  legend: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default Value
