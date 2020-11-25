import PropTypes from 'prop-types'

import { constants, spacing } from '../Styles'

import { useScroller } from '../Scroll/helpers'

const ModelMatrixLabels = ({ matrix, settings: { cellDimension } }) => {
  const rowRef = useScroller({
    namespace: 'ModelMatrixHorizontal',
    isWheelEmitter: true,
    isWheelListener: true,
  })

  return (
    <div
      ref={rowRef}
      css={{
        flex: 1,
        display: 'flex',
        overflow: 'hidden',
        borderBottom: constants.borders.regular.coal,
      }}
    >
      {matrix.labels.map((label) => (
        <div
          key={label}
          css={{
            minWidth: cellDimension,
            maxWidth: cellDimension,
            padding: spacing.normal,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            borderRight: constants.borders.regular.coal,
          }}
        >
          {label}
        </div>
      ))}
    </div>
  )
}

ModelMatrixLabels.propTypes = {
  matrix: PropTypes.shape({
    name: PropTypes.string.isRequired,
    overallAccuracy: PropTypes.number.isRequired,
    labels: PropTypes.arrayOf(PropTypes.string).isRequired,
    matrix: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)).isRequired,
  }).isRequired,
  settings: PropTypes.shape({
    cellDimension: PropTypes.number.isRequired,
  }).isRequired,
}

export default ModelMatrixLabels
