import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import { SCROLLBAR_RESIZE_HANDLE_SIZE } from './helpers'

let origin
let scrollbarTrackWidth
let scrollbarWidth

const TimelineScrollbarRightHandle = ({ scrollbarRef, scrollbarTrackRef }) => {
  /* istanbul ignore next */
  const handleMouseMove = ({ clientX }) => {
    const difference = clientX - origin

    const newZoom = ((scrollbarWidth + difference) / scrollbarTrackWidth) * 100

    // prevent handles from overlapping
    const minZoom =
      ((SCROLLBAR_RESIZE_HANDLE_SIZE * 2) / scrollbarTrackWidth) * 100

    const clampedZoom = Math.max(minZoom, Math.min(newZoom, 100))

    /* eslint-disable no-param-reassign */
    scrollbarRef.current.style.width = `${clampedZoom}%`
  }

  /* istanbul ignore next */
  const handleMouseUp = () => {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX }) => {
    origin = clientX

    scrollbarWidth = scrollbarRef.current?.getBoundingClientRect().width || 0

    scrollbarTrackWidth =
      scrollbarTrackRef.current?.getBoundingClientRect().width || 0

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
