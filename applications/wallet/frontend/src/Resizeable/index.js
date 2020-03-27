import PropTypes from 'prop-types'
import { DraggableCore } from 'react-draggable'

import useLocalStorage from '../LocalStorage'

import { zIndex } from '../Styles'

import { calculateDelta } from './helpers'

const DRAG_WIDTH = 20
const DRAG_OFFSET = -10

// Only supports horizontal drag on the left of an element
const Resizeable = ({ children, initialWidth, storageName }) => {
  const [width, setWidth] = useLocalStorage({
    key: storageName,
    initialValue: initialWidth,
  })

  const onDrag = calculateDelta({ width, setWidth })

  return (
    <div style={{ display: 'flex', alignItems: 'stretch', flexWrap: 'nowrap' }}>
      <DraggableCore onDrag={onDrag}>
        <div
          css={{
            cursor: 'col-resize',
            width: DRAG_WIDTH,
            marginLeft: DRAG_OFFSET,
            marginRight: DRAG_OFFSET,
            zIndex: zIndex.layout.interactive,
          }}
        />
      </DraggableCore>
      <div css={{ flex: 1, width }}>{children}</div>
    </div>
  )
}

Resizeable.propTypes = {
  children: PropTypes.node.isRequired,
  initialWidth: PropTypes.number.isRequired,
  storageName: PropTypes.string.isRequired,
}

export default Resizeable
