import PropTypes from 'prop-types'
import { useEffect, useRef } from 'react'

import { colors, constants, spacing, zIndex } from '../Styles'

import { getScroller } from '../Scroll/helpers'

import { setScrollbarScrollableWidth } from './helpers'

let origin
let scrollbarOrigin
let scrollbarScrollableWidth
let manualScrollbarTrackWidth
let manualScrollbarWidth
const scrollbarZoom = 100

export const SCROLLBAR_CONTAINER_HEIGHT = 36
const RESIZE_HANDLE_SIZE = 20

const TimelineScrollbar = ({ width, zoom, rulerRef }) => {
  const horizontalScroller = getScroller({ namespace: 'Timeline' })
  const scrollbarTrackRef = useRef()
  const scrollbarRef = useRef()

  const horizontalScrollerDeregister = horizontalScroller.register({
    eventName: 'scroll',
    callback: /* istanbul ignore next */ ({ node }) => {
      if (!scrollbarRef.current || !node) return

      scrollbarScrollableWidth = setScrollbarScrollableWidth({
        scrollbarRef,
        zoom,
      })

      // the scrollLeft value when the timeline is scrolled all the way to the end
      const maxScrollLeft = node.scrollWidth - node.offsetWidth

      // compute scrollLeft as a percentage to translate to scrollbar scrollLeft
      const fractionScrolled =
        maxScrollLeft === 0 ? maxScrollLeft : node.scrollLeft / maxScrollLeft

      scrollbarRef.current.style.left = `${
        fractionScrolled * scrollbarScrollableWidth
      }px`
    },
  })

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

    scrollbarScrollableWidth = setScrollbarScrollableWidth({
      scrollbarRef,
      zoom,
    })

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const onRightHandleMouseMove = ({ clientX }) => {
    const difference = clientX - origin

    const newZoom =
      ((manualScrollbarWidth + difference) / manualScrollbarTrackWidth) * 100

    const minZoom = ((RESIZE_HANDLE_SIZE * 2) / manualScrollbarTrackWidth) * 100

    const clampedZoom = Math.max(minZoom, Math.min(100, newZoom))

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
    scrollbarOrigin = scrollbarRef.current.offsetLeft

    const { width: scrollbarWidth = 0 } =
      scrollbarRef.current?.getBoundingClientRect() || {}

    manualScrollbarWidth = scrollbarWidth

    const { width: trackWidth = 0 } =
      scrollbarTrackRef.current?.getBoundingClientRect() || {}

    manualScrollbarTrackWidth = trackWidth

    scrollbarScrollableWidth = setScrollbarScrollableWidth({
      scrollbarRef,
      zoom,
    })

    document.addEventListener('mousemove', onRightHandleMouseMove)
    document.addEventListener('mouseup', onRightHandleMouseUp)
  }

  useEffect(() => {
    return () => {
      horizontalScrollerDeregister()
    }
  }, [horizontalScrollerDeregister])

  return (
    <>
      <div
        css={{
          height: SCROLLBAR_CONTAINER_HEIGHT,
          backgroundColor: colors.structure.soot,
          marginLeft: -width,
          width,
        }}
      />
      <div
        css={{
          position: 'absolute',
          bottom: 0,
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          height: SCROLLBAR_CONTAINER_HEIGHT,
          backgroundColor: colors.structure.soot,
          zIndex: zIndex.timeline.tracks + 1,
          paddingLeft: spacing.small,
          paddingRight: spacing.small,
          paddingTop: spacing.moderate,
          paddingBottom: spacing.moderate,
        }}
      >
        <div
          ref={scrollbarTrackRef}
          css={{
            position: 'relative',
            width: '100%',
            height: '100%',
            backgroundColor: colors.structure.coal,
            borderRadius: constants.borderRadius.large,
            border: constants.borders.regular.smoke,
          }}
        >
          <div
            ref={scrollbarRef}
            css={{
              display: 'flex',
              position: 'absolute',
              width: `${scrollbarZoom}%`,
              height: '100%',
              backgroundColor: colors.structure.smoke,
              borderRadius: constants.borderRadius.medium,
            }}
          >
            <div
              css={{
                backgroundColor: colors.structure.steel,
                width: RESIZE_HANDLE_SIZE,
                borderTopLeftRadius: constants.borderRadius.medium,
                borderBottomLeftRadius: constants.borderRadius.medium,
              }}
            />
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
            <div
              role="button"
              tabIndex="-1"
              aria-label="Timeline Scrollbar Resize Handle"
              css={{
                backgroundColor: colors.structure.steel,
                width: RESIZE_HANDLE_SIZE,
                borderTopRightRadius: constants.borderRadius.medium,
                borderBottomRightRadius: constants.borderRadius.medium,
                ':hover, :active': { backgroundColor: colors.structure.pebble },
              }}
              onMouseDown={onRightHandleMouseDown}
            />
          </div>
        </div>
      </div>
    </>
  )
}

TimelineScrollbar.propTypes = {
  width: PropTypes.number.isRequired,
  zoom: PropTypes.number.isRequired,
  rulerRef: PropTypes.shape({
    current: PropTypes.shape({
      offsetWidth: PropTypes.number,
      scrollWidth: PropTypes.number,
    }),
  }).isRequired,
}

export default TimelineScrollbar
