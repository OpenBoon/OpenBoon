/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import useLocalStorage from '../LocalStorage'

import { zIndex } from '../Styles'

const DRAG_WIDTH = 20

let originX

const Resizeable = ({
  minWidth,
  storageName,
  position,
  onMouseUp,
  children,
}) => {
  const [width, setWidth] = useLocalStorage({
    key: storageName,
    initialValue: minWidth,
  })

  const direction = position === 'right' ? 1 : -1

  /* istanbul ignore next */
  const handleMouseMove = ({ clientX }) => {
    setWidth({ value: width - (clientX - originX) * direction })
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientX }) => {
    const finalWidth = width - (clientX - originX) * direction

    setWidth({ value: Math.max(minWidth, finalWidth) })

    onMouseUp({ width: finalWidth })

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
    <div style={{ display: 'flex', alignItems: 'stretch', flexWrap: 'nowrap' }}>
      {position === 'left' && (
        <div
          css={{
            userSelect: 'none',
            cursor: 'col-resize',
            width: DRAG_WIDTH,
            marginLeft: -DRAG_WIDTH / 2,
            marginRight: -DRAG_WIDTH / 2,
            zIndex: zIndex.layout.interactive,
          }}
          onMouseDown={handleMouseDown}
        />
      )}
      <div css={{ flex: 1, width }}>{children}</div>
      {position === 'right' && (
        <div
          css={{
            userSelect: 'none',
            cursor: 'col-resize',
            width: DRAG_WIDTH,
            marginLeft: -DRAG_WIDTH / 2,
            marginRight: -DRAG_WIDTH / 2,
            zIndex: zIndex.layout.interactive,
          }}
          onMouseDown={handleMouseDown}
        />
      )}
    </div>
  )
}

Resizeable.propTypes = {
  minWidth: PropTypes.number.isRequired,
  storageName: PropTypes.string.isRequired,
  position: PropTypes.oneOf(['left', 'right']).isRequired,
  onMouseUp: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default Resizeable
