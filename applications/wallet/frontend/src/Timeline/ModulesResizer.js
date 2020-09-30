/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import { zIndex } from '../Styles'

import { MODULES_MIN_WIDTH, ACTIONS } from './reducer'

const DRAG_WIDTH = 4

let originX

const TimelineModulesResizer = ({ settings: { modulesWidth }, dispatch }) => {
  /* istanbul ignore next */
  const handleMouseMove = ({ clientX }) => {
    const newWidth = modulesWidth - (clientX - originX) * -1

    dispatch({
      type: ACTIONS.RESIZE_MODULES,
      payload: { value: Math.max(MODULES_MIN_WIDTH, newWidth) },
    })
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientX }) => {
    const finalWidth = modulesWidth - (clientX - originX) * -1

    dispatch({
      type: ACTIONS.RESIZE_MODULES,
      payload: { value: Math.max(MODULES_MIN_WIDTH, finalWidth) },
    })

    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX }) => {
    originX = clientX

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseup', handleMouseUp)
  }

  return (
    <div
      css={{
        position: 'absolute',
        userSelect: 'none',
        cursor: 'col-resize',
        top: 0,
        bottom: 0,
        width: DRAG_WIDTH,
        marginLeft: -DRAG_WIDTH / 2,
        marginRight: -DRAG_WIDTH / 2,
        zIndex: zIndex.layout.interactive,
      }}
      onMouseDown={handleMouseDown}
    />
  )
}

TimelineModulesResizer.propTypes = {
  settings: PropTypes.shape({
    filter: PropTypes.string.isRequired,
    modulesWidth: PropTypes.number.isRequired,
    modules: PropTypes.shape({}).isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineModulesResizer
