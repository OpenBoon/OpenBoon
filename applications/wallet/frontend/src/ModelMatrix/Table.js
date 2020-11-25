import PropTypes from 'prop-types'

// TODO: fetch data
import matrix from './__mocks__/matrix'

import { useScroller } from '../Scroll/helpers'

import ModelMatrixRow from './Row'

export const LABELS_WIDTH = 100

const ZOOM = 1

const ModelMatrixTable = ({ width, height }) => {
  const tableRef = useScroller({
    namespace: 'ModelMatrixVertical',
    isWheelEmitter: true,
    isWheelListener: true,
  })

  const cellDimension = (height / matrix.labels.length) * ZOOM

  return (
    <div
      ref={tableRef}
      css={{
        flex: 1,
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
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
}

export default ModelMatrixTable
