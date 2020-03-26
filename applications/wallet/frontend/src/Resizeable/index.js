import { useRef, useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import { DraggableCore } from 'react-draggable'

import debounce from '../Debounce/helpers'

// Only supports horizontal drag towards the left
const Resizeable = ({ children }) => {
  const [size, setSize] = useState(400)

  const contentRef = useRef()
  const wrapperRef = useRef()

  const validateSize = debounce(() => {
    const { current: { parentElement: { clientWidth } = {} } = {} } = wrapperRef

    if (size > clientWidth) {
      setSize(clientWidth)
    }
  }, 100)

  useEffect(() => {
    if (size === 0) {
      const content = contentRef.current
      const actualContent = content.children[0]
      const initialSize = actualContent.offsetWidth

      setSize(initialSize)
    }

    validateSize()
  }, [size, validateSize])

  const onDrag = (e, { deltaX }) => {
    setSize(Math.max(10, size - deltaX))
  }

  const onStop = () => {
    validateSize()
  }

  return (
    <div
      ref={wrapperRef}
      style={{ display: 'flex', alignItems: 'stretch', flexFlow: 'row nowrap' }}
    >
      <DraggableCore key="handle" onDrag={onDrag} onStop={onStop}>
        <div
          css={{
            cursor: 'ew-resize',
            width: 20,
            marginLeft: -10,
            marginRight: -10,
            background: 'transparent',
            display: 'flex',
            zIndex: 10,
            alignItems: 'center',
            alignContent: 'center',
            justifyContent: 'center',
          }}
        >
          <div
            css={{
              cursor: 'ew-resize',
              width: 12,
              height: 50,
              background: 'white',
              border: '2px solid lightgray',
              borderRadius: 8,
              textAlign: 'center',
              zUndex: 10,
              overflow: 'hidden',
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <span
              css={{
                display: 'inline-block',
                overflow: 'hidden',
                fontSize: 12,
                fontWeight: 'bold',
                fontFamily: 'sans-serif',
                letterSpacing: 1,
                color: '#b3b3b3',
                textShadow: '1px 0 1px rgb(90, 90, 90)',
                lineHeight: 4,
                ':after': {
                  content: '. . . . . . . .',
                },
              }}
            />
          </div>
        </div>
      </DraggableCore>
      <div ref={contentRef} css={{ flexFlow: 'row', width: size }}>
        {children}
      </div>
    </div>
  )
}

Resizeable.propTypes = {
  children: PropTypes.node.isRequired,
}

export default Resizeable
