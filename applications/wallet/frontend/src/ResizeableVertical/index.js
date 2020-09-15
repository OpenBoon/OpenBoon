/* eslint-disable jsx-a11y/no-static-element-interactions */
import { useState } from 'react'
import PropTypes from 'prop-types'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { zIndex } from '../Styles'

const DRAG_SIZE = 4
const DIRECTION = 1

let originY

const ResizeableVertical = ({
  storageName,
  minExpandedSize,
  header,
  children,
}) => {
  const [size, setSize] = useLocalStorageState({
    key: storageName,
    initialValue: minExpandedSize,
  })

  const [originSize, setOriginSize] = useState(minExpandedSize)

  const [isOpen, setIsOpen] = useState(false)

  const toggleOpen = () => {
    // Updating here allows for proper collapse/expand messaging
    if (isOpen) {
      setOriginSize(0)
    }
    if (!isOpen) {
      setOriginSize(minExpandedSize)
    }

    setIsOpen(!isOpen)
  }

  /* istanbul ignore next */
  const handleMouseMove = ({ clientY }) => {
    const difference = clientY - originY

    const newSize = (isOpen ? size : 0) - difference * DIRECTION

    setSize({ value: newSize })

    setIsOpen(newSize > 0)
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientY }) => {
    const difference = clientY - originY

    const newSize = (isOpen ? size : 0) - difference * DIRECTION

    // Always update size if greater than minExpandedSize
    if (newSize > minExpandedSize) {
      // Prevent size being set smaller than minExpandedSize
      setSize({ value: Math.max(minExpandedSize, newSize) })

      setOriginSize(Math.max(minExpandedSize, newSize))
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

      {header({ toggleOpen })}

      <div
        css={{
          height: isOpen ? size : 0,
        }}
      >
        {children({
          size,
          originSize,
        })}
      </div>
    </div>
  )
}

ResizeableVertical.propTypes = {
  storageName: PropTypes.string.isRequired,
  minExpandedSize: PropTypes.number.isRequired,
  header: PropTypes.func.isRequired,
  children: PropTypes.func.isRequired,
}

export default ResizeableVertical
