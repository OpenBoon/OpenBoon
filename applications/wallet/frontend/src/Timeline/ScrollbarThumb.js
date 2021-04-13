import PropTypes from 'prop-types'

import { useEffect } from 'react'

import { colors } from '../Styles'

import { getScroller } from '../Scroll/helpers'

import { getScrollbarScrollableWidth } from './helpers'

let origin
let scrollbarOrigin
let scrollbarScrollableWidth

export const SCROLLBAR_CONTAINER_HEIGHT = 36

const TimelineScrollbarThumb = ({ zoom, scrollbarRef, rulerRef }) => {
  const horizontalScroller = getScroller({ namespace: 'Timeline' })

  const horizontalScrollerDeregister = horizontalScroller.register({
    eventName: 'scroll',
    callback: /* istanbul ignore next */ ({ node }) => {
      if (!scrollbarRef.current || !node) return

      scrollbarScrollableWidth = getScrollbarScrollableWidth({
        scrollbarRef,
        zoom,
      })

      // the scrollLeft value when the timeline is scrolled all the way to the end
      const maxScrollLeft = node.scrollWidth - node.offsetWidth

      // compute scrollLeft as a percentage to translate to scrollbar scrollLeft
      const fractionScrolled =
        maxScrollLeft === 0 ? maxScrollLeft : node.scrollLeft / maxScrollLeft

      /* eslint-disable no-param-reassign */
      scrollbarRef.current.style.left = `${
        fractionScrolled * scrollbarScrollableWidth
      }px`
    },
  })

  useEffect(() => {
    return () => {
      horizontalScrollerDeregister()
    }
  }, [horizontalScrollerDeregister])

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
      zoom,
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
  zoom: PropTypes.number.isRequired,
  scrollbarRef: PropTypes.shape({
    current: PropTypes.shape({
      offsetLeft: PropTypes.number,
      style: PropTypes.shape({ left: PropTypes.string }),
    }),
  }).isRequired,
  rulerRef: PropTypes.shape({
    current: PropTypes.shape({
      offsetWidth: PropTypes.number,
      scrollWidth: PropTypes.number,
    }),
  }).isRequired,
}

export default TimelineScrollbarThumb
