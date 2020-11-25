/* eslint-disable jsx-a11y/no-static-element-interactions */
import { useRef, useCallback } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, zIndex } from '../Styles'

import { updatePlayheadPosition, GUIDE_WIDTH } from './helpers'
import { getScroller } from '../Scroll/helpers'

const HEIGHT = 12
const WIDTH = 10
const SCROLL_BUFFER = 50

let originX
let originLeft

const TimelinePlayhead = ({ videoRef, rulerRef, zoom, followPlayhead }) => {
  const playheadRef = useRef()
  const frameRef = useRef()

  const video = videoRef.current

  const scroller = getScroller({ namespace: 'timeline' })

  /* istanbul ignore next */
  const onMount = useCallback(
    (node) => {
      const animate = () => {
        if (!video) return

        const { scrollWidth = 0, scrollLeft = 0, clientWidth = 0 } =
          rulerRef.current || {}

        const hiddenToTheRight = scrollWidth - scrollLeft - clientWidth > 0

        const currentPosition = node.offsetLeft + WIDTH / 2 - GUIDE_WIDTH / 2

        const nextPosition =
          (((video.currentTime / video.duration) * zoom) / 100) * clientWidth -
          GUIDE_WIDTH / 2

        updatePlayheadPosition({
          video,
          playhead: node,
          zoom,
          scrollLeft,
        })

        const isOutOfView =
          currentPosition < 0 ||
          (currentPosition > clientWidth - SCROLL_BUFFER && hiddenToTheRight)

        if (isOutOfView && !video.paused && followPlayhead) {
          scroller.emit({
            eventName: 'scroll',
            data: {
              scrollX: nextPosition,
              scrollY: 0,
            },
          })
        }

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
    [video, zoom, rulerRef, scroller, followPlayhead],
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
      paused: PropTypes.bool.isRequired,
    }),
  }).isRequired,
  rulerRef: PropTypes.shape({
    current: PropTypes.shape({
      scrollWidth: PropTypes.number.isRequired,
      scrollLeft: PropTypes.number.isRequired,
      clientWidth: PropTypes.number.isRequired,
    }),
  }).isRequired,
  zoom: PropTypes.number.isRequired,
  followPlayhead: PropTypes.bool.isRequired,
}

export default TimelinePlayhead
