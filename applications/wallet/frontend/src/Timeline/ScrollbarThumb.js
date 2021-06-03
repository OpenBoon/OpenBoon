import PropTypes from 'prop-types'

import { colors } from '../Styles'

import { getScrollbarScrollableWidth } from './helpers'

let origin
let scrollbarOrigin
let scrollbarScrollableWidth

const TimelineScrollbarThumb = ({
  rulerRef,
  scrollbarRef,
  scrollbarTrackRef,
  horizontalScroller,
}) => {
  /* istanbul ignore next */
  const handleMouseMove = ({ clientX }) => {
    const difference = clientX - origin

    const fractionScrolled =
      scrollbarScrollableWidth === 0
        ? 0
        : (scrollbarOrigin + difference) / scrollbarScrollableWidth

    // the max number of pixels the ruler scroll left
    const rulerScrollableWidth =
      rulerRef.current.scrollWidth - rulerRef.current.offsetWidth

    horizontalScroller.emit({
      eventName: 'scroll',
      data: {
        scrollX: rulerScrollableWidth * fractionScrolled,
      },
    })
  }

  /* istanbul ignore next */
  const handleMouseUp = () => {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const handleMouseDown = ({ clientX }) => {
    origin = clientX
    scrollbarOrigin = scrollbarRef.current.offsetLeft

    scrollbarScrollableWidth = getScrollbarScrollableWidth({
      scrollbarRef,
      scrollbarTrackRef,
    })

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  return (
    <div
      role="button"
      tabIndex="-1"
      aria-label="Timeline Scrollbar"
      onMouseDown={handleMouseDown}
      css={{
        flex: 1,
        ':hover, :active': { backgroundColor: colors.structure.steel },
      }}
    />
  )
}

TimelineScrollbarThumb.propTypes = {
  rulerRef: PropTypes.shape({
    current: PropTypes.shape({
      offsetWidth: PropTypes.number,
      scrollWidth: PropTypes.number,
    }),
  }).isRequired,
  scrollbarRef: PropTypes.shape({
    current: PropTypes.shape({
      offsetLeft: PropTypes.number,
      style: PropTypes.shape({ left: PropTypes.string }),
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
}

export default TimelineScrollbarThumb
