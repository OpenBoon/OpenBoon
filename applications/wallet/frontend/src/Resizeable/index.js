import { useRef, useState } from 'react'
import PropTypes from 'prop-types'
import { DraggableCore } from 'react-draggable'

import { calculateDelta } from './helpers'

// Only supports horizontal drag on the left of an element
const Resizeable = ({ children, initialWidth }) => {
  const [width, setWidth] = useState(initialWidth)

  const contentRef = useRef()
  const wrapperRef = useRef()

  const onDrag = calculateDelta({ width, setWidth })

  return (
    <div
      ref={wrapperRef}
      style={{ display: 'flex', alignItems: 'stretch', flexWrap: 'nowrap' }}
    >
      <DraggableCore key="handle" onDrag={onDrag}>
        <div
          css={{
            cursor: 'ew-resize',
            width: 20,
            marginLeft: -10,
            marginRight: -10,
            zIndex: 10,
          }}
        />
      </DraggableCore>
      <div ref={contentRef} css={{ flex: 1, width }}>
        {children}
      </div>
    </div>
  )
}

Resizeable.propTypes = {
  children: PropTypes.node.isRequired,
  initialWidth: PropTypes.number.isRequired,
}

export default Resizeable
