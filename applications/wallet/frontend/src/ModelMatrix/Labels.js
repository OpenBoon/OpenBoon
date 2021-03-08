import PropTypes from 'prop-types'

import { constants } from '../Styles'

import { useScroller } from '../Scroll/helpers'

import ModelMatrixLabel from './Label'

const ModelMatrixLabels = ({ matrix, settings: { height, zoom } }) => {
  const rowRef = useScroller({
    namespace: 'ModelMatrixHorizontal',
    isWheelEmitter: true,
    isWheelListener: true,
  })

  const cellDimension = (height / matrix.labels.length) * zoom

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
        <ModelMatrixLabel
          key={label}
          cellDimension={cellDimension}
          label={label}
        />
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
    height: PropTypes.number.isRequired,
    zoom: PropTypes.number.isRequired,
  }).isRequired,
}

export default ModelMatrixLabels
