/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'
import { useState } from 'react'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { zIndex } from '../Styles'

import { getFinalSize } from './helpers'

export const noop = () => {}

const DRAG_SIZE = 4

let originCoord

const Resizeable = ({
  minExpandedSize,
  minCollapsedSize,
  storageName,
  openToThe,
  onMouseUp,
  children,
}) => {
  const [size, setSize] = useLocalStorageState({
    key: storageName,
    initialValue: minExpandedSize,
  })
  const [startingSize, setStartingSize] = useState(minExpandedSize)

  const direction = openToThe === 'left' || openToThe === 'top' ? 1 : -1

  /* istanbul ignore next */
  const handleMouseMove = ({ clientX, clientY }) => {
    const difference = (openToThe === 'top' ? clientY : clientX) - originCoord

    const newSize = size - difference * direction

    setSize({
      value: minCollapsedSize ? Math.max(minCollapsedSize, newSize) : newSize,
    })
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientX, clientY }) => {
    const difference = (openToThe === 'top' ? clientY : clientX) - originCoord

    // Calculate new size
    const newSize = size - difference * direction

    // Calculate if there is a snap size
    const finalSize = getFinalSize({
      startingSize,
      newSize,
      minExpandedSize,
      minCollapsedSize,
    })

    // Update startingSize
    setStartingSize(finalSize)

    setSize({
      value: Math.max(minCollapsedSize || minExpandedSize, finalSize),
    })

    //
    onMouseUp({ size: finalSize })

    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX, clientY }) => {
    originCoord = openToThe === 'top' ? clientY : clientX

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
      <div css={{ [openToThe === 'top' ? 'height' : 'width']: size }}>
        {children({ size, setSize, setStartingSize })}
      </div>
      {openToThe === 'right' && (
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
    </div>
  )
}

Resizeable.defaultProps = {
  onMouseUp: noop,
}

Resizeable.propTypes = {
  minExpandedSize: PropTypes.number.isRequired,
  minCollapsedSize: PropTypes.number.isRequired,
  storageName: PropTypes.string.isRequired,
  openToThe: PropTypes.oneOf(['left', 'right', 'top']).isRequired,
  onMouseUp: PropTypes.func,
  children: PropTypes.func.isRequired,
}

export default Resizeable
