/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'
import { useState } from 'react'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { zIndex } from '../Styles'

import { getFinalSize } from './helpers'

const DRAG_SIZE = 4

let originAxis

const Resizeable = ({
  minExpandedSize,
  minCollapsedSize,
  storageName,
  openToThe,
  onMouseUp,
  render,
}) => {
  const [size, setSize] = useLocalStorageState({
    key: storageName,
    initialValue: minExpandedSize,
  })
  const [startingAxis, setStartingAxis] = useState(minExpandedSize)

  const direction = openToThe === 'left' || openToThe === 'top' ? 1 : -1

  /* istanbul ignore next */
  const handleMouseMove = ({ clientX, clientY }) => {
    const difference = (openToThe === 'top' ? clientY : clientX) - originAxis

    const sizeCalculation = size - difference * direction

    setSize({
      value: minCollapsedSize
        ? Math.max(minCollapsedSize, sizeCalculation)
        : sizeCalculation,
    })
  }

  /* istanbul ignore next */
  const handleMouseUp = ({ clientX, clientY }) => {
    const difference = (openToThe === 'top' ? clientY : clientX) - originAxis

    const sizeCalculation = size - difference * direction

    const finalSize = getFinalSize({
      startingAxis,
      sizeCalculation,
      minExpandedSize,
      minCollapsedSize,
    })

    setStartingAxis(finalSize)

    setSize({
      value: minCollapsedSize
        ? Math.max(minCollapsedSize, finalSize)
        : Math.max(minExpandedSize, finalSize),
    })

    onMouseUp({ size: finalSize })

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
          aria-label="Resize to the left"
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
          aria-label="Resize to the top"
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
        {render({ size, setSize, setStartingAxis })}
      </div>
      {openToThe === 'right' && (
        <div
          aria-label="Resize to the right"
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
  minExpandedSize: PropTypes.number.isRequired,
  minCollapsedSize: PropTypes.number.isRequired,
  storageName: PropTypes.string.isRequired,
  openToThe: PropTypes.oneOf(['left', 'right', 'top']).isRequired,
  onMouseUp: PropTypes.func.isRequired,
  render: PropTypes.func.isRequired,
}

export default Resizeable
