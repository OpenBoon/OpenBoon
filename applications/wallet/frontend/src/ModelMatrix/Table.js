import { useEffect } from 'react'
import PropTypes from 'prop-types'

import { useScroller } from '../Scroll/helpers'
import ModelMatrixResize from './Resize'

import ModelMatrixRow from './Row'

const ModelMatrixTable = ({ matrix, width, height, settings, dispatch }) => {
  const tableRef = useScroller({
    namespace: 'ModelMatrixVertical',
    isWheelEmitter: true,
    isWheelListener: true,
    isScrollEmitter: true,
  })

  useEffect(() => {
    dispatch({ width, height })
  }, [dispatch, width, height])

  /* istanbul ignore next */
  useEffect(() => {
    setTimeout(() => {
      tableRef.current.dispatchEvent(new Event('scroll'))
    }, 0)
  }, [settings.zoom, tableRef])

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
            settings={settings}
            label={label}
            index={index}
          />
        )
      })}

      <ModelMatrixResize
        matrix={matrix}
        settings={settings}
        dispatch={dispatch}
      />
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
  settings: PropTypes.shape({
    zoom: PropTypes.number.isRequired,
    isMinimapOpen: PropTypes.bool.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelMatrixTable
