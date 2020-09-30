/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import { useLocalStorage } from '../LocalStorage/helpers'

import { zIndex } from '../Styles'

import ResizeableVerticalDropMessage from './DropMessage'

const DRAG_SIZE = 4
const DIRECTION = 1

let originY

const reducer = (state, action) => ({ ...state, ...action })

const ResizeableVertical = ({ storageName, minHeight, header, children }) => {
  const [state, dispatch] = useLocalStorage({
    key: storageName,
    reducer,
    initialState: {
      size: minHeight,
      originSize: 0,
      isOpen: false,
    },
  })

  const { size, originSize, isOpen } = state

  const toggleOpen = () => {
    dispatch({
      originSize: state.isOpen ? 0 : minHeight,
      isOpen: !state.isOpen,
    })
  }

  /* istanbul ignore next */
  const handleMouseMove = ({ clientY }) => {
    const difference = clientY - originY

    const newSize = (isOpen ? size : 0) - difference * DIRECTION

    dispatch({ size: newSize, isOpen: newSize > 0 })
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientY }) => {
    const difference = clientY - originY

    const newSize = (isOpen ? size : 0) - difference * DIRECTION

    if (newSize > minHeight) {
      dispatch({ size: newSize })
    }

    if (newSize < minHeight) {
      dispatch({
        size: minHeight,
        originSize: isOpen ? 0 : minHeight,
        isOpen: !isOpen,
      })
    }

    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientY }) => {
    originY = clientY

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseup', handleMouseUp)
  }

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'stretch',
        flexWrap: 'nowrap',
      }}
    >
      <div
        aria-label="Resize vertically"
        css={{
          userSelect: 'none',
          cursor: 'row-resize',
          height: DRAG_SIZE,
          marginTop: -DRAG_SIZE / 2,
          marginBottom: -DRAG_SIZE / 2,
          zIndex: zIndex.layout.interactive,
        }}
        onMouseDown={handleMouseDown}
      />

      {header({ isOpen, toggleOpen })}

      <div
        css={{
          height: isOpen ? size : 0,
        }}
      >
        {size >= minHeight ? (
          children({ size })
        ) : (
          /* istanbul ignore next */ <ResizeableVerticalDropMessage
            size={size}
            originSize={originSize}
          />
        )}
      </div>
    </div>
  )
}

ResizeableVertical.propTypes = {
  storageName: PropTypes.string.isRequired,
  minHeight: PropTypes.number.isRequired,
  header: PropTypes.func.isRequired,
  children: PropTypes.func.isRequired,
}

export default ResizeableVertical
