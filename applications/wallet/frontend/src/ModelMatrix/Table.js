import PropTypes from 'prop-types'

// TODO: fetch data
import matrix from './__mocks__/matrix'

import { colors, constants, spacing, typography } from '../Styles'

export const LABELS_WIDTH = 100

const ZOOM = 1

const ModelMatrixTable = ({ width, height }) => {
  const cellDimension = (height / matrix.labels.length) * ZOOM

  return (
    <div
      css={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        width,
        height,
        overflow: 'scroll',
      }}
    >
      {matrix.labels.map((label, row) => {
        const rowTotal = matrix.matrix[row].reduce(
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
                  row === matrix.labels.length - 1
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
              css={{
                flex: 1,
                display: 'flex',
                overflow: 'hidden',
              }}
            >
              {matrix.matrix[row].map((value, col) => {
                return (
                  <div
                    key={matrix.labels[col]}
                    css={{ backgroundColor: colors.structure.white }}
                  >
                    <div
                      css={{
                        width: cellDimension,
                        height: '100%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        // TODO: use correct blue gradient
                        backgroundColor: 'blue',
                        opacity: value / rowTotal,
                      }}
                    >
                      {(value / rowTotal) * 100}%
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )
      })}
    </div>
  )
}

ModelMatrixTable.propTypes = {
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
}

export default ModelMatrixTable
