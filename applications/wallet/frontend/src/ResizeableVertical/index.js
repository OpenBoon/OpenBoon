/* eslint-disable jsx-a11y/no-static-element-interactions */
import { useState } from 'react'
import PropTypes from 'prop-types'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { zIndex } from '../Styles'

const DRAG_SIZE = 4
const DIRECTION = 1

let originY

const ResizeableVertical = ({ storageName, minHeight, header, children }) => {
  const [size, setSize] = useLocalStorageState({
    key: `${storageName}.height`,
    initialValue: minHeight,
  })

  const [isOpen, setIsOpen] = useLocalStorageState({
    key: `${storageName}.isOpen`,
    initialValue: false,
  })

  const [originSize, setOriginSize] = useState(minHeight)

  const toggleOpen = () => {
    // Updating here allows for proper collapse/expand messaging
    if (isOpen) {
      setOriginSize(0)
    }
    if (!isOpen) {
      setOriginSize(minHeight)
    }

    setIsOpen({ value: !isOpen })
  }

  /* istanbul ignore next */
  const handleMouseMove = ({ clientY }) => {
    const difference = clientY - originY

    const newSize = (isOpen ? size : 0) - difference * DIRECTION

    setSize({ value: newSize })

    setIsOpen({ value: newSize > 0 })
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientY }) => {
    const difference = clientY - originY

    const newSize = (isOpen ? size : 0) - difference * DIRECTION

    // Always update size if greater than minHeight
    if (newSize > minHeight) {
      // Prevent size being set smaller than minHeight
      setSize({ value: Math.max(minHeight, newSize) })

      setOriginSize(Math.max(minHeight, newSize))
    } else {
      // Dragging open from closed state below minHeight should snap open
      if (!isOpen) {
        setSize({ value: minHeight })

        toggleOpen()
      }

      // Dragging close under minHeight should snap close
      if (isOpen) {
        // If a user drags below the minHeight and drops to collapse,
        // the size is still set under the minHeight. This resets it.
        setSize({ value: Math.max(minHeight, size) })

        toggleOpen()
      }
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
        overflow: 'hidden',
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
        {children({
          size,
          isOpen: size >= minHeight,
          originSize,
        })}
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
