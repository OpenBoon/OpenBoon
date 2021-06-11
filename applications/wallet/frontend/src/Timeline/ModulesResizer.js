/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import ResizeableHandle from '../Resizeable/Handle'

import { MIN_WIDTH, ACTIONS } from './reducer'

const TimelineModulesResizer = ({ width, dispatch }) => {
  /* istanbul ignore next */
  const handleMouseMove = ({ difference }) => {
    const newWidth = width - difference

    dispatch({
      type: ACTIONS.RESIZE_MODULES,
      payload: { value: Math.max(MIN_WIDTH, newWidth) },
    })
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ difference }) => {
    const finalWidth = width - difference

    dispatch({
      type: ACTIONS.RESIZE_MODULES,
      payload: { value: Math.max(MIN_WIDTH, finalWidth) },
    })
  }

  return (
    <ResizeableHandle
      openToThe="right"
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      styles={{
        position: 'absolute',
        top: 0,
        bottom: 0,
      }}
    />
  )
}

TimelineModulesResizer.propTypes = {
  width: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineModulesResizer
