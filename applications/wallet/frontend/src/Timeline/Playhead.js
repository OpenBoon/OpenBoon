/* eslint-disable jsx-a11y/no-static-element-interactions */
import { useRef, useCallback } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, zIndex } from '../Styles'

import { updatePlayheadPosition, GUIDE_WIDTH } from './helpers'

const HEIGHT = 12
const WIDTH = 10

let originX
let originLeft

const TimelinePlayhead = ({ videoRef, rulerRef, zoom }) => {
  const playheadRef = useRef()
  const frameRef = useRef()

  const video = videoRef.current

  /* istanbul ignore next */
  const onMount = useCallback(
    (node) => {
      const animate = () => {
        const offset = rulerRef.current?.scrollLeft || 0

        updatePlayheadPosition({
          video,
          playhead: node,
          zoom,
          offset,
        })

        frameRef.current = requestAnimationFrame(animate)
      }

      if (frameRef.current && !node) {
        cancelAnimationFrame(frameRef.current)
        frameRef.current = null
        playheadRef.current = null
      }

      if (!frameRef.current && node) {
        animate()
        playheadRef.current = node
      }
    },
    [video, zoom, rulerRef],
  )

  /* istanbul ignore next */
  const handleMouseMove = ({ clientX }) => {
    const maxPosition =
      playheadRef.current.parentNode.offsetWidth * (zoom / 100) -
      GUIDE_WIDTH / 2

    const newPosition = Math.min(
      Math.max(0, originLeft + clientX - originX),
      maxPosition,
    )

    const newCurrentTime = (newPosition / maxPosition) * video.duration

    video.currentTime = newCurrentTime
  }

  /* istanbul ignore next */
  const handleMouseUp = () => {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX }) => {
    originX = clientX
    originLeft = playheadRef.current.offsetLeft + rulerRef.current.scrollLeft

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  return (
    <div
      ref={onMount}
      onMouseDown={handleMouseDown}
      css={{
        userSelect: 'none',
        cursor: 'col-resize',
        position: 'absolute',
        marginTop: 0,
        top: constants.timeline.rulerRowHeight - HEIGHT,
        bottom: 0,
        marginLeft: -(WIDTH / 2) + constants.borderWidths.regular / 2,
        width: WIDTH,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        zIndex: zIndex.timeline.playhead,
      }}
    >
      <div
        css={{
          borderStyle: 'solid',
          borderWidth: `${HEIGHT}px ${WIDTH}px 0 ${WIDTH}px`,
          borderColor: `${colors.signal.sky.base} transparent transparent transparent`,
        }}
      />
      <div
        css={{
          flex: 1,
          width: GUIDE_WIDTH,
          backgroundColor: colors.signal.sky.base,
        }}
      />
    </div>
  )
}

TimelinePlayhead.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      currentTime: PropTypes.number.isRequired,
      duration: PropTypes.number.isRequired,
    }),
  }).isRequired,
  rulerRef: PropTypes.shape({
    current: PropTypes.shape({
      scrollLeft: PropTypes.number.isRequired,
    }),
  }).isRequired,
  zoom: PropTypes.number.isRequired,
}

export default TimelinePlayhead
