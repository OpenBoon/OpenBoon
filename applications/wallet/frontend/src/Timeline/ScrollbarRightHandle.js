import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

let origin
let manualScrollbarTrackWidth
let manualScrollbarWidth

export const SCROLLBAR_CONTAINER_HEIGHT = 36
const RESIZE_HANDLE_SIZE = 20

const TimelineScrollbarRightHandle = ({ scrollbarRef, scrollbarTrackRef }) => {
  /* istanbul ignore next */
  const onRightHandleMouseMove = ({ clientX }) => {
    const difference = clientX - origin

    const newZoom =
      ((manualScrollbarWidth + difference) / manualScrollbarTrackWidth) * 100

    // prevent handles from overlapping
    const minZoom = ((RESIZE_HANDLE_SIZE * 2) / manualScrollbarTrackWidth) * 100

    const clampedZoom = Math.max(minZoom, Math.min(100, newZoom))

    /* eslint-disable no-param-reassign */
    scrollbarRef.current.style.width = `${clampedZoom}%`
  }

  /* istanbul ignore next */
  const onRightHandleMouseUp = () => {
    document.removeEventListener('mousemove', onRightHandleMouseMove)
    document.removeEventListener('mouseup', onRightHandleMouseUp)
  }

  /* istanbul ignore next */
  const onRightHandleMouseDown = ({ clientX }) => {
    origin = clientX

    const { width: scrollbarWidth = 0 } =
      scrollbarRef.current?.getBoundingClientRect() || {}

    manualScrollbarWidth = scrollbarWidth

    const { width: trackWidth = 0 } =
      scrollbarTrackRef.current?.getBoundingClientRect() || {}

    manualScrollbarTrackWidth = trackWidth

    document.addEventListener('mousemove', onRightHandleMouseMove)
    document.addEventListener('mouseup', onRightHandleMouseUp)
  }

  return (
    <button
      type="button"
      tabIndex="-1"
      aria-label="Timeline Scrollbar Resize Handle"
      css={{
        backgroundColor: colors.structure.steel,
        width: RESIZE_HANDLE_SIZE,
        border: 0,
        borderTopRightRadius: constants.borderRadius.medium,
        borderBottomRightRadius: constants.borderRadius.medium,
        ':hover, :active': { backgroundColor: colors.structure.pebble },
      }}
      onMouseDown={onRightHandleMouseDown}
    />
  )
}

TimelineScrollbarRightHandle.propTypes = {
  scrollbarRef: PropTypes.shape({
    current: PropTypes.shape({
      offsetLeft: PropTypes.number,
      style: PropTypes.shape({ width: PropTypes.string }),
    }),
  }).isRequired,
  scrollbarTrackRef: PropTypes.shape({
    current: PropTypes.shape({
      getBoundingClientRect: PropTypes.func.isRequired,
    }),
  }).isRequired,
}

export default TimelineScrollbarRightHandle
