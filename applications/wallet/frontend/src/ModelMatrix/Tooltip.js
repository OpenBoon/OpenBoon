import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

const ModelMatrixTooltip = ({
  matrix,
  index,
  value,
  rowTotal,
  percent,
  col,
}) => (
  <div
    css={{
      color: colors.structure.coal,
      backgroundColor: colors.structure.white,
      borderRadius: constants.borderRadius.small,
      boxShadow: constants.boxShadows.default,
      padding: spacing.moderate,
    }}
  >
    <h3>
      <span
        css={{
          fontFamily: typography.family.condensed,
          fontWeight: typography.weight.regular,
          color: colors.structure.iron,
        }}
      >
        Predictions:
      </span>{' '}
      {value}/{rowTotal}({Math.round(percent)}%)
    </h3>
    <h3>
      <span
        css={{
          fontFamily: typography.family.condensed,
          fontWeight: typography.weight.regular,
          color: colors.structure.iron,
        }}
      >
        Label True:
      </span>{' '}
      {matrix.labels[index]}
    </h3>
    <h3>
      <span
        css={{
          fontFamily: typography.family.condensed,
          fontWeight: typography.weight.regular,
          color: colors.structure.iron,
        }}
      >
        Label Pred:
      </span>{' '}
      {matrix.labels[col]}
    </h3>
  </div>
)

ModelMatrixTooltip.propTypes = {
  matrix: PropTypes.shape({
    labels: PropTypes.arrayOf(PropTypes.string).isRequired,
  }).isRequired,
  index: PropTypes.number.isRequired,
  value: PropTypes.number.isRequired,
  rowTotal: PropTypes.number.isRequired,
  percent: PropTypes.number.isRequired,
  col: PropTypes.number.isRequired,
}

export default ModelMatrixTooltip
