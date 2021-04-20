import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import {
  SCROLLBAR_RESIZE_HANDLE_SIZE,
  SCROLLBAR_TRACK_MARGIN_WIDTH,
} from './helpers'

let origin
let scrollbarTrackWidth
let scrollbarWidth
let scrollbarOffsetLeft
let scrollbarRight
let maxScrollbarRight
let pointerToRightEdgeDiff

const TimelineScrollbarRightHandle = ({ scrollbarRef, scrollbarTrackRef }) => {
  /* istanbul ignore next */
  const handleMouseMove = ({ clientX }) => {
    const rightDifference = clientX - origin

    // clamp left handle difference when it touches left edge of track
    const leftDifference = Math.min(scrollbarOffsetLeft, rightDifference)

    const newWidth = scrollbarWidth + rightDifference + leftDifference

    // width when the scrollbar thumb is 0
    const minWidth = SCROLLBAR_RESIZE_HANDLE_SIZE * 2

    const maxWidth = scrollbarTrackWidth - SCROLLBAR_TRACK_MARGIN_WIDTH * 2

    const clampedWidth = Math.max(minWidth, Math.min(maxWidth, newWidth))

    // calculate when right handle is touching right edge of the track
    const isMaxExpandedToRight =
      scrollbarRight + rightDifference > maxScrollbarRight

    // the distance the pointer drags the handle
    // beyond the right edge of the track
    const displacedLeftOffset = isMaxExpandedToRight
      ? clientX + pointerToRightEdgeDiff - maxScrollbarRight
      : 0

    const minWidthLeftOffset =
      scrollbarWidth / 2 - SCROLLBAR_RESIZE_HANDLE_SIZE + scrollbarOffsetLeft

    // prevent scroll when scrollbar is at minWidth
    const newOffsetLeft =
      clampedWidth === minWidth
        ? minWidthLeftOffset
        : Math.max(
            0,
            scrollbarOffsetLeft - rightDifference - displacedLeftOffset,
          )

    /* eslint-disable no-param-reassign */
    scrollbarRef.current.style.width = `${clampedWidth}px`
    scrollbarRef.current.style.left = `${newOffsetLeft}px`
  }

  /* istanbul ignore next */
  const handleMouseUp = () => {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX }) => {
    origin = clientX

    scrollbarOffsetLeft = scrollbarRef.current?.offsetLeft

    const {
      width: sbWidth = 0,
      right: sbRight = 0,
    } = scrollbarRef.current?.getBoundingClientRect()

    scrollbarWidth = sbWidth
    scrollbarRight = sbRight

    const {
      width: trackWidth = 0,
      right: trackRight = 0,
    } = scrollbarTrackRef.current?.getBoundingClientRect()

    scrollbarTrackWidth = trackWidth
    maxScrollbarRight = trackRight - SCROLLBAR_TRACK_MARGIN_WIDTH
    pointerToRightEdgeDiff = scrollbarRight - clientX

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  return (
    <button
      type="button"
      aria-label="Timeline Scrollbar Resize Handle"
      css={{
        backgroundColor: colors.structure.steel,
        width: SCROLLBAR_RESIZE_HANDLE_SIZE,
        border: 0,
        borderTopRightRadius: constants.borderRadius.medium,
        borderBottomRightRadius: constants.borderRadius.medium,
        ':hover, :active': { backgroundColor: colors.structure.pebble },
      }}
      onMouseDown={handleMouseDown}
    />
  )
}

TimelineScrollbarRightHandle.propTypes = {
  scrollbarRef: PropTypes.shape({
    current: PropTypes.shape({
      offsetLeft: PropTypes.number,
      style: PropTypes.shape({
        left: PropTypes.string,
        width: PropTypes.string,
      }),
    }),
  }).isRequired,
  scrollbarTrackRef: PropTypes.shape({
    current: PropTypes.shape({
      getBoundingClientRect: PropTypes.func.isRequired,
    }),
  }).isRequired,
}

export default TimelineScrollbarRightHandle
