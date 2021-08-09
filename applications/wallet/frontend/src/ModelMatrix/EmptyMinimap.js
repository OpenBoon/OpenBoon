import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

const EMPTY_MATRIX = [0, 1, 2, 3]

const ModelMatrixEmptyMinimap = ({ isMatrixApplicable }) => {
  return (
    <div
      css={{
        position: 'relative',
        border: isMatrixApplicable
          ? constants.borders.large.steel
          : constants.borders.large.smoke,
        padding: spacing.hairline,
        borderRadius: constants.borderRadius.small,
        display: 'grid',
        gridTemplate: `repeat(${EMPTY_MATRIX.length}, 1fr) / repeat(${EMPTY_MATRIX.length}, 1fr)`,
      }}
    >
      {EMPTY_MATRIX.map((row) => {
        return EMPTY_MATRIX.map((col) => {
          return (
            <div
              key={`${row}${col}`}
              css={{
                [`:not(:nth-of-type(4n))`]: {
                  borderRight: isMatrixApplicable
                    ? constants.borders.regular.coal
                    : constants.borders.regular.smoke,
                },
                [`:not(:nth-of-type(n + 13))`]: {
                  borderBottom: isMatrixApplicable
                    ? constants.borders.regular.coal
                    : constants.borders.regular.smoke,
                },
                paddingBottom: '100%',
                backgroundColor: isMatrixApplicable
                  ? colors.structure.smoke
                  : colors.structure.coal,
              }}
            />
          )
        })
      })}
    </div>
  )
}

ModelMatrixEmptyMinimap.propTypes = {
  isMatrixApplicable: PropTypes.bool.isRequired,
}

export default ModelMatrixEmptyMinimap
