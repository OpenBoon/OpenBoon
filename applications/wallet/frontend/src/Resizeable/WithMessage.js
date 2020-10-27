/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import { useLocalStorage } from '../LocalStorage/helpers'

import { zIndex } from '../Styles'

import ResizeableWithMessageDropMessage from './DropMessage'

const DRAG_SIZE = 4
const DIRECTION = 1

let origin

const reducer = (state, action) => ({ ...state, ...action })

const ResizeableWithMessage = ({
  storageName,
  minSize,
  openToThe,
  isInitiallyOpen,
  header,
  children,
}) => {
  const [state, dispatch] = useLocalStorage({
    key: storageName,
    reducer,
    initialState: {
      size: minSize,
      originSize: isInitiallyOpen ? minSize : 0,
      isOpen: isInitiallyOpen,
    },
  })

  const { size, originSize, isOpen } = state

  const isHorizontal = openToThe === 'left'

  const toggleOpen = () => {
    dispatch({
      originSize: state.isOpen ? 0 : minSize,
      isOpen: !state.isOpen,
    })
  }

  /* istanbul ignore next */
  const handleMouseMove = ({ clientX, clientY }) => {
    const difference = (isHorizontal ? clientX : clientY) - origin

    const newSize = (isOpen ? size : 0) - difference * DIRECTION

    dispatch({ size: newSize, isOpen: newSize > 0 })
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientX, clientY }) => {
    const difference = (isHorizontal ? clientX : clientY) - origin

    const newSize = (isOpen ? size : 0) - difference * DIRECTION

    if (newSize > minSize) {
      dispatch({ size: newSize, isOpen: true })
    }

    if (newSize < minSize) {
      dispatch({
        size: minSize,
        originSize: isOpen ? 0 : minSize,
        isOpen: !isOpen,
      })
    }

    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX, clientY }) => {
    origin = isHorizontal ? clientX : clientY

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseup', handleMouseUp)
  }

  return (
    <div
      style={{
        display: 'flex',
        [isHorizontal ? 'height' : 'width']: '100%',
        flexDirection: isHorizontal ? 'row' : 'column',
        alignItems: 'stretch',
        flexWrap: 'nowrap',
      }}
    >
      {openToThe === 'left' && (
        <div
          aria-label="Resize horizontally"
          css={{
            userSelect: 'none',
            cursor: 'col-resize',
            width: DRAG_SIZE,
            marginLeft: -DRAG_SIZE / 2,
            marginRight: -DRAG_SIZE / 2,
            zIndex: zIndex.layout.interactive,
          }}
          onMouseDown={handleMouseDown}
        />
      )}

      {openToThe === 'top' && (
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
      )}

      {header({ isOpen, toggleOpen })}

      <div
        css={{
          [isHorizontal ? 'width' : 'height']: isOpen ? size : 0,
          ...(isHorizontal
            ? {
                display: 'flex',
                flexDirection: 'column',
              }
            : {}),
        }}
      >
        {size >= minSize ? (
          children({ size })
        ) : (
          /* istanbul ignore next */ <ResizeableWithMessageDropMessage
            size={size}
            originSize={originSize}
          />
        )}
      </div>
    </div>
  )
}

ResizeableWithMessage.propTypes = {
  storageName: PropTypes.string.isRequired,
  minSize: PropTypes.number.isRequired,
  openToThe: PropTypes.oneOf(['top', 'left']).isRequired,
  isInitiallyOpen: PropTypes.bool.isRequired,
  header: PropTypes.func.isRequired,
  children: PropTypes.func.isRequired,
}

export default ResizeableWithMessage
