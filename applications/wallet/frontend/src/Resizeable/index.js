/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { zIndex } from '../Styles'

const DRAG_SIZE = 4

let originAxis

const Resizeable = ({
  minSize,
  storageName,
  openToThe,
  onMouseUp,
  children,
  childFixedSize,
}) => {
  const [size, setSize] = useLocalStorageState({
    key: storageName,
    initialValue: minSize,
  })

  const direction = openToThe === 'left' || openToThe === 'top' ? 1 : -1

  /* istanbul ignore next */
  const handleMouseMove = ({ clientX, clientY }) => {
    const difference = (openToThe === 'top' ? clientY : clientX) - originAxis

    const sizeCalculation = size - difference * direction

    setSize({
      value: childFixedSize
        ? Math.max(minSize, sizeCalculation)
        : sizeCalculation,
    })
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientX, clientY }) => {
    const difference = (openToThe === 'top' ? clientY : clientX) - originAxis

    const sizeCalculation = size - difference * direction

    const finalValue =
      sizeCalculation < childFixedSize ? minSize : sizeCalculation

    setSize({
      value: finalValue,
    })

    onMouseUp({ size: finalValue })

    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX, clientY }) => {
    originAxis = openToThe === 'top' ? clientY : clientX

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseup', handleMouseUp)
  }

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: openToThe === 'top' ? 'column' : 'row',
        alignItems: 'stretch',
        flexWrap: 'nowrap',
        overflow: 'hidden',
      }}
    >
      {openToThe === 'left' && (
        <div
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
      <div css={{ [openToThe === 'top' ? 'height' : 'width']: size }}>
        {children}
      </div>
      {openToThe === 'right' && (
        <div
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
    </div>
  )
}

Resizeable.propTypes = {
  minSize: PropTypes.number.isRequired,
  storageName: PropTypes.string.isRequired,
  openToThe: PropTypes.oneOf(['left', 'right', 'top']).isRequired,
  onMouseUp: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
  childFixedSize: PropTypes.number.isRequired,
}

export default Resizeable
