import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import {
  SCROLLBAR_RESIZE_HANDLE_SIZE,
  SCROLLBAR_TRACK_MARGIN_WIDTH,
  setIgnore,
} from './helpers'

import { ACTIONS } from './reducer'

let origin
let scrollbarTrackWidth
let scrollbarWidth
let scrollbarOffsetLeft
let scrollbarRight
let maxScrollbarRight
let pointerToRightEdgeDiff

const TimelineScrollbarHandle = ({
  scrollbarRef,
  scrollbarTrackRef,
  horizontalScroller,
  isLeft,
  dispatch,
}) => {
  /* istanbul ignore next */
  const handleMouseMove = ({ clientX }) => {
    const direction = isLeft ? -1 : 1

    const selectedHandleDifference = (clientX - origin) * direction

    const maxSelectedHandleDifference = isLeft
      ? maxScrollbarRight - scrollbarRight
      : scrollbarOffsetLeft

    // clamp opposite handle difference when selected handle touches edge of track
    const oppositeHandleDifference = Math.min(
      maxSelectedHandleDifference,
      selectedHandleDifference,
    )

    const newWidth =
      scrollbarWidth + selectedHandleDifference + oppositeHandleDifference

    // width when the scrollbar thumb is 0
    const minWidth = SCROLLBAR_RESIZE_HANDLE_SIZE * 2

    const maxWidth = scrollbarTrackWidth - SCROLLBAR_TRACK_MARGIN_WIDTH * 2

    const clampedWidth = Math.max(minWidth, Math.min(newWidth, maxWidth))

    // calculate when right handle is touching right edge of the track
    const isMaxExpandedToRight =
      scrollbarRight + selectedHandleDifference > maxScrollbarRight

    // the distance the pointer drags the handle
    // beyond the right edge of the track
    const displacedOffset =
      isMaxExpandedToRight && !isLeft
        ? clientX + pointerToRightEdgeDiff - maxScrollbarRight
        : 0

    const minWidthLeftOffset =
      scrollbarWidth / 2 - SCROLLBAR_RESIZE_HANDLE_SIZE + scrollbarOffsetLeft

    const computedOffsetLeft =
      scrollbarOffsetLeft - selectedHandleDifference - displacedOffset

    // prevent scroll when scrollbar is at minWidth
    const newOffsetLeft =
      clampedWidth === minWidth
        ? minWidthLeftOffset
        : Math.max(0, computedOffsetLeft)

    /* eslint-disable no-param-reassign */
    scrollbarRef.current.style.width = `${(clampedWidth / maxWidth) * 100}%`
    scrollbarRef.current.style.left = `${(newOffsetLeft / maxWidth) * 100}%`

    const newZoom = (1 / (clampedWidth / maxWidth)) * 100

    dispatch({ type: ACTIONS.ZOOM, payload: { value: newZoom } })

    horizontalScroller.emit({
      eventName: 'scroll',
      data: {
        scrollX: (newZoom * newOffsetLeft) / 100,
        scrollY: 0,
      },
    })
  }

  /* istanbul ignore next */
  const handleMouseUp = () => {
    setIgnore({ value: false })
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX }) => {
    setIgnore({ value: true })

    origin = clientX

    scrollbarOffsetLeft = scrollbarRef.current?.offsetLeft

    const { width: sbWidth = 0, right: sbRight = 0 } =
      scrollbarRef.current?.getBoundingClientRect()

    scrollbarWidth = sbWidth
    scrollbarRight = sbRight

    const { width: trackWidth = 0, right: trackRight = 0 } =
      scrollbarTrackRef.current?.getBoundingClientRect()

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
        [isLeft ? 'borderTopLeftRadius' : 'borderTopRightRadius']:
          constants.borderRadius.medium,
        [isLeft ? 'borderBottomLeftRadius' : 'borderBottomRightRadius']:
          constants.borderRadius.medium,
        ':hover, :active': { backgroundColor: colors.structure.pebble },
      }}
      onMouseDown={handleMouseDown}
    />
  )
}

TimelineScrollbarHandle.propTypes = {
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
  horizontalScroller: PropTypes.shape({
    emit: PropTypes.func.isRequired,
  }).isRequired,
  isLeft: PropTypes.bool.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineScrollbarHandle
