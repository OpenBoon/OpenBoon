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

const TimelineScrollbarLeftHandle = ({ scrollbarRef, scrollbarTrackRef }) => {
  /* istanbul ignore next */
  const handleMouseMove = ({ clientX }) => {
    const leftDifference = origin - clientX

    // clamp right handle difference when it touches right edge of track
    const rightDifference = Math.min(
      maxScrollbarRight - scrollbarRight,
      leftDifference,
    )

    const newWidth = scrollbarWidth + leftDifference + rightDifference

    // width when the scrollbar thumb is 0
    const minWidth = SCROLLBAR_RESIZE_HANDLE_SIZE * 2

    const maxWidth = scrollbarTrackWidth - SCROLLBAR_TRACK_MARGIN_WIDTH * 2

    const clampedWidth = Math.max(minWidth, Math.min(maxWidth, newWidth))

    const minWidthLeftOffset =
      scrollbarWidth / 2 - SCROLLBAR_RESIZE_HANDLE_SIZE + scrollbarOffsetLeft

    // prevent scroll when scrollbar is at minWidth
    const newOffsetLeft =
      clampedWidth === minWidth
        ? minWidthLeftOffset
        : Math.max(0, scrollbarOffsetLeft - leftDifference)

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
        borderTopLeftRadius: constants.borderRadius.medium,
        borderBottomLeftRadius: constants.borderRadius.medium,
        ':hover, :active': { backgroundColor: colors.structure.pebble },
      }}
      onMouseDown={handleMouseDown}
    />
  )
}

TimelineScrollbarLeftHandle.propTypes = {
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

export default TimelineScrollbarLeftHandle
