/* eslint-disable jsx-a11y/no-static-element-interactions */
import { useState } from 'react'
import PropTypes from 'prop-types'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { zIndex } from '../Styles'

import ResizeableMk2Copy from './Copy'

const DRAG_SIZE = 4

let originCoord

const ResizeableMk2 = ({
  storageName,
  minExpandedSize,
  collapsedSize,
  openToThe,
  children,
}) => {
  const [size, setSize] = useLocalStorageState({
    key: storageName,
    initialValue: minExpandedSize,
  })

  const [startingSize, setStartingSize] = useState(minExpandedSize)

  const [isOpen, setIsOpen] = useState(false)

  const toggleOpen = () => {
    // Updating here allows for proper collapse/expand messaging
    if (isOpen) {
      setStartingSize(collapsedSize)
    }
    if (!isOpen) {
      setStartingSize(minExpandedSize)
    }

    setIsOpen(!isOpen)
  }

  const isVertical = openToThe === 'top'
  const direction = openToThe === 'left' || openToThe === 'top' ? 1 : -1

  /* istanbul ignore next */
  const handleMouseMove = ({ clientX, clientY }) => {
    const difference = (isVertical ? clientY : clientX) - originCoord

    const newSize = (isOpen ? size : collapsedSize) - difference * direction

    // Prevent dragging smaller than collapsedSize
    setSize({ value: Math.max(collapsedSize, newSize) })

    setIsOpen(newSize > collapsedSize)
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientX, clientY }) => {
    const difference = (isVertical ? clientY : clientX) - originCoord

    const newSize = (isOpen ? size : collapsedSize) - difference * direction

    // Always update size if greater than minExpandedSize
    if (newSize > minExpandedSize) {
      // Prevent size being set smaller than minExpandedSize
      setSize({ value: Math.max(minExpandedSize, newSize) })

      setStartingSize(Math.max(minExpandedSize, newSize))
    } else {
      // Dragging open from closed state below minExpandedSize should snap open
      if (!isOpen) {
        setSize({ value: minExpandedSize })

        toggleOpen()
      }

      // Dragging close under minExpandedSize should snap close
      if (isOpen) {
        // If a user drags below the minExpandedSize and drops to collapse,
        // the size is still set under the minExpandedSize. This resets it.
        setSize({ value: Math.max(minExpandedSize, size) })

        toggleOpen()
      }
    }

    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX, clientY }) => {
    originCoord = isVertical ? clientY : clientX

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

      <div
        css={{
          [isVertical ? 'height' : 'width']: isOpen ? size : collapsedSize,
        }}
      >
        {children({
          size,
          toggleOpen,
          renderCopy: () => (
            <ResizeableMk2Copy
              size={size}
              startingSize={startingSize}
              minExpandedSize={minExpandedSize}
            />
          ),
        })}
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

ResizeableMk2.propTypes = {
  storageName: PropTypes.string.isRequired,
  minExpandedSize: PropTypes.number.isRequired,
  collapsedSize: PropTypes.number.isRequired,
  openToThe: PropTypes.oneOf(['left', 'right', 'top']).isRequired,
  children: PropTypes.func.isRequired,
}

export default ResizeableMk2
