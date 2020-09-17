/* eslint-disable jsx-a11y/no-static-element-interactions */
import { useRef, useEffect } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, zIndex } from '../Styles'

import { updatePlayheadPosition, GUIDE_WIDTH } from './helpers'

const DRAG_WIDTH = 10

let originX
let originLeft

const TimelinePlayhead = ({ videoRef }) => {
  const playheadRef = useRef()
  const frameRef = useRef()

  const video = videoRef.current
  const playhead = playheadRef.current

  /* istanbul ignore next */
  useEffect(() => {
    const animate = () => {
      updatePlayheadPosition({ video, playhead })

      frameRef.current = requestAnimationFrame(animate)
    }

    frameRef.current = requestAnimationFrame(animate)

    return () => cancelAnimationFrame(frameRef.current)
  }, [video, playhead])

  /* istanbul ignore next */
  const handleMouseMove = ({ clientX }) => {
    const maxPosition = playhead.parentNode.offsetWidth - GUIDE_WIDTH / 2

    const newPosition = Math.min(
      Math.max(0, originLeft + clientX - originX),
      maxPosition,
    )

    const newCurrentTime = (newPosition / maxPosition) * video.duration

    video.currentTime = newCurrentTime
  }

  /* istanbul ignore next */
  const handleMouseUp = () => {
    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX }) => {
    originX = clientX
    originLeft = playhead.offsetLeft

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseup', handleMouseUp)
  }

  return (
    <div
      ref={playheadRef}
      onMouseDown={handleMouseDown}
      css={{
        userSelect: 'none',
        cursor: 'col-resize',
        position: 'absolute',
        marginTop: 0,
        top:
          constants.timeline.rulerRowHeight - constants.timeline.playheadHeight,
        bottom: 0,
        marginLeft: -DRAG_WIDTH / 2,
        width: DRAG_WIDTH,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        zIndex: zIndex.layout.interactive,
      }}
    >
      <div
        css={{
          borderStyle: 'solid',
          borderWidth: `${constants.timeline.playheadHeight}px ${
            constants.timeline.playheadWidth / 2
          }px 0 ${constants.timeline.playheadWidth / 2}px`,
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
}

export default TimelinePlayhead
