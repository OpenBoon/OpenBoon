/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import stylesShape from '../Styles/shape'

import { zIndex } from '../Styles'

const DRAG_SIZE = 4

let origin

const ORIENTATION = {
  HORIZONTAL: {
    cursor: 'col-resize',
    width: DRAG_SIZE,
    marginLeft: -DRAG_SIZE / 2,
    marginRight: -DRAG_SIZE / 2,
    zIndex: zIndex.layout.interactive,
  },

  VERTICAL: {
    cursor: 'row-resize',
    height: DRAG_SIZE,
    marginTop: -DRAG_SIZE / 2,
    marginBottom: -DRAG_SIZE / 2,
    zIndex: zIndex.layout.interactive,
  },
}

const VARIANTS = {
  left: ORIENTATION.HORIZONTAL,
  right: ORIENTATION.HORIZONTAL,
  top: ORIENTATION.VERTICAL,
  bottom: ORIENTATION.VERTICAL,
}

const ResizeableHandle = ({ onMouseMove, onMouseUp, openToThe, styles }) => {
  const isHorizontal = openToThe === 'left' || openToThe === 'right'

  const direction = openToThe === 'left' || openToThe === 'top' ? 1 : -1

  /* istanbul ignore next */
  const handleMouseMove = ({ clientX, clientY }) => {
    const difference = ((isHorizontal ? clientX : clientY) - origin) * direction

    onMouseMove({ difference })
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientX, clientY }) => {
    const difference = ((isHorizontal ? clientX : clientY) - origin) * direction

    onMouseUp({ difference })

    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX, clientY }) => {
    origin = isHorizontal ? clientX : clientY

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  return (
    <div
      aria-label={`Resize ${isHorizontal ? 'horizontally' : 'vertically'}`}
      css={{
        userSelect: 'none',
        zIndex: zIndex.layout.interactive,
        ...VARIANTS[openToThe],
        ...styles,
      }}
      onMouseDown={handleMouseDown}
    />
  )
}

ResizeableHandle.defaultProps = {
  styles: {},
}

ResizeableHandle.propTypes = {
  onMouseMove: PropTypes.func.isRequired,
  onMouseUp: PropTypes.func.isRequired,
  openToThe: PropTypes.oneOf(['left', 'right', 'top', 'bottom']).isRequired,
  styles: stylesShape,
}

export default ResizeableHandle
