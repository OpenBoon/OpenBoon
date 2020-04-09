/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import useLocalStorage from '../LocalStorage'

import { zIndex } from '../Styles'

const DRAG_WIDTH = 20

let originX

const Resizeable = ({ initialWidth, minWidth, storageName, children }) => {
  const [width, setWidth] = useLocalStorage({
    key: storageName,
    initialValue: initialWidth,
  })

  /* istanbul ignore next */
  const handleMouseMove = ({ clientX }) => {
    setWidth({ value: Math.max(minWidth, width - (clientX - originX)) })
  }

  /* istanbul ignore next */
  const handleMouseUp = () => {
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
      <div css={{ flex: 1, width }}>{children}</div>
    </div>
  )
}

Resizeable.propTypes = {
  initialWidth: PropTypes.number.isRequired,
  minWidth: PropTypes.number.isRequired,
  storageName: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default Resizeable
