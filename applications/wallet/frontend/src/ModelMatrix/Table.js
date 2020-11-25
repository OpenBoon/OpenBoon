import PropTypes from 'prop-types'
import { useEffect } from 'react'

import { useScroller } from '../Scroll/helpers'

import ModelMatrixRow from './Row'

export const LABELS_WIDTH = 100

const ZOOM = 1

const ModelMatrixTable = ({ matrix, width, height, dispatch }) => {
  const tableRef = useScroller({
    namespace: 'ModelMatrixVertical',
    isWheelEmitter: true,
    isWheelListener: true,
  })

  const cellDimension = (height / matrix.labels.length) * ZOOM

  useEffect(() => {
    dispatch({ width, cellDimension })
  }, [dispatch, width, cellDimension])

  return (
    <div
      ref={tableRef}
      css={{
        display: 'flex',
        flexDirection: 'column',
        width,
        height,
        overflow: 'hidden',
      }}
    >
      {matrix.labels.map((label, index) => {
        return (
          <ModelMatrixRow
            key={label}
            matrix={matrix}
            cellDimension={cellDimension}
            label={label}
            index={index}
          />
        )
      })}
    </div>
  )
}

ModelMatrixTable.propTypes = {
  matrix: PropTypes.shape({
    name: PropTypes.string.isRequired,
    overallAccuracy: PropTypes.number.isRequired,
    labels: PropTypes.arrayOf(PropTypes.string).isRequired,
    matrix: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)).isRequired,
  }).isRequired,
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelMatrixTable
