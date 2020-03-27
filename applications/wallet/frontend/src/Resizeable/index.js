import PropTypes from 'prop-types'
import { DraggableCore } from 'react-draggable'

import useLocalStorage from '../LocalStorage'

import { calculateDelta } from './helpers'

// Only supports horizontal drag on the left of an element
const Resizeable = ({ children, initialWidth, storageName }) => {
  const [width, setWidth] = useLocalStorage(storageName, initialWidth)

  const onDrag = calculateDelta({ width, setWidth })

  return (
    <div style={{ display: 'flex', alignItems: 'stretch', flexWrap: 'nowrap' }}>
      <DraggableCore onDrag={onDrag}>
        <div
          css={{
            cursor: 'col-resize',
            width: 20,
            marginLeft: -10,
            marginRight: -10,
            zIndex: 10,
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
