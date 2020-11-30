import PropTypes from 'prop-types'
import { useEffect } from 'react'

import { useScroller } from '../Scroll/helpers'
import ModelMatrixResize from './Resize'

import ModelMatrixRow from './Row'

export const LABELS_WIDTH = 100

const ModelMatrixTable = ({ matrix, width, height, zoom, dispatch }) => {
  const tableRef = useScroller({
    namespace: 'ModelMatrixVertical',
    isWheelEmitter: true,
    isWheelListener: true,
  })

  useEffect(() => {
    dispatch({ width, height })
  }, [dispatch, width, height])

  const cellDimension = (height / matrix.labels.length) * zoom

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

      <ModelMatrixResize zoom={zoom} dispatch={dispatch} />
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
  zoom: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelMatrixTable
