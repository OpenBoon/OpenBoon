import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import { useScroller } from '../Scroll/helpers'

import { getColor } from './helpers'

export const LABELS_WIDTH = 100
const CONTRAST_THRESHOLD = 69

const ModelMatrixRow = ({ matrix, cellDimension, label, index }) => {
  const rowRef = useScroller({
    namespace: 'ModelMatrixHorizontal',
    isWheelEmitter: true,
    isWheelListener: true,
  })

  const rowTotal = matrix.matrix[index].reduce(
    (previous, current) => previous + current,
    0,
  )

  return (
    <div
      key={label}
      css={{
        display: 'flex',
        height: cellDimension,
      }}
    >
      <div
        css={{
          width: LABELS_WIDTH,
          paddingLeft: spacing.normal,
          display: 'flex',
          alignItems: 'center',
          fontFamily: typography.family.condensed,
          fontWeight: typography.weight.bold,
          borderRight: constants.borders.regular.coal,
          borderBottom:
            index === matrix.labels.length - 1
              ? constants.borders.regular.transparent
              : constants.borders.regular.coal,
        }}
      >
        {label}{' '}
        <span
          css={{
            color: colors.structure.zinc,
            paddingLeft: spacing.small,
            fontWeight: typography.weight.regular,
          }}
        >
          ({rowTotal})
        </span>
      </div>

      <div
        ref={rowRef}
        css={{
          flex: 1,
          display: 'flex',
          overflow: 'hidden',
        }}
      >
        {matrix.matrix[index].map((value, col) => {
          const percent = (value / rowTotal) * 100

          return (
            <div key={matrix.labels[col]}>
              <div
                css={{
                  width: cellDimension,
                  height: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  backgroundColor: getColor({ percent }),
                  color:
                    percent > CONTRAST_THRESHOLD
                      ? colors.structure.white
                      : colors.structure.coal,
                }}
              >
                {Math.round(percent)}%
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

ModelMatrixRow.propTypes = {
  matrix: PropTypes.shape({
    name: PropTypes.string.isRequired,
    overallAccuracy: PropTypes.number.isRequired,
    labels: PropTypes.arrayOf(PropTypes.string).isRequired,
    matrix: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)).isRequired,
  }).isRequired,
  cellDimension: PropTypes.number.isRequired,
  label: PropTypes.string.isRequired,
  index: PropTypes.number.isRequired,
}

export default ModelMatrixRow