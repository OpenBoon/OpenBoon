import PropTypes from 'prop-types'
import { useEffect, useRef } from 'react'

import { colors, constants, spacing, zIndex } from '../Styles'

import { getScroller } from '../Scroll/helpers'

import { getNextScrollLeft } from './helpers'
import { ACTIONS } from './reducer'

let origin
let scrollbarOrigin
let scrollbarTrackWidth
let scrollbarScrollableWidth
let nextScrollLeft
let newScrollbarScrollableWidth

export const SCROLLBAR_CONTAINER_HEIGHT = 36
const RESIZE_HANDLE_SIZE = 20

const TimelineScrollbar = ({ settings, rulerRef, dispatch }) => {
  const horizontalScroller = getScroller({ namespace: 'Timeline' })
  const scrollbarRef = useRef()

  const setScrollbarScrollableWidth = () => {
    const { width: scrollbarWidth = 0 } =
      scrollbarRef.current?.getBoundingClientRect() || {}

    scrollbarTrackWidth = scrollbarWidth * (settings.zoom / 100)

    // the max number of pixels the scrollbar thumb can travel
    scrollbarScrollableWidth = scrollbarTrackWidth - scrollbarWidth
  }

  const horizontalScrollerDeregister = horizontalScroller.register({
    eventName: 'scroll',
    callback: /* istanbul ignore next */ ({ node }) => {
      if (!scrollbarRef.current || !node) return

      setScrollbarScrollableWidth()

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

    setScrollbarScrollableWidth()

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  /* istanbul ignore next */
  const onRightHandleMouseMove = ({ clientX }) => {
    const difference = clientX - origin

    newScrollbarScrollableWidth = scrollbarScrollableWidth - difference

    const zoom = 100 / (1 - newScrollbarScrollableWidth / scrollbarTrackWidth)

    console.log(zoom)
    dispatch({
      type: ACTIONS.ZOOM,
      payload: { value: zoom },
    })

    const fractionScrolled =
      newScrollbarScrollableWidth === 0
        ? 0
        : scrollbarOrigin / newScrollbarScrollableWidth

    // the max number of pixels the ruler scroll left
    const rulerScrollableWidth =
      rulerRef.current.scrollWidth - rulerRef.current.offsetWidth

    // const nextWidth =
    //   (rulerRef.current.scrollWidth / settings.zoom) * zoom -
    //   rulerRef.current.offsetWidth

    const rulerFraction =
      rulerRef.current.scrollLeft /
      (rulerRef.current.scrollWidth - rulerRef.current.offsetWidth)

    nextScrollLeft =
      rulerScrollableWidth * Math.min(1, Math.max(0, fractionScrolled))

    // // keep scrollbarOrigin in place
    // horizontalScroller.emit({
    //   eventName: 'scroll',
    //   data: {
    //     scrollX: nextScrollLeft,
    //   },
    // })
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

    setScrollbarScrollableWidth()

    document.addEventListener('mousemove', onRightHandleMouseMove)
    document.addEventListener('mouseup', onRightHandleMouseUp)
  }

  useEffect(() => {
    return () => {
      horizontalScrollerDeregister()
    }
  }, [
    horizontalScrollerDeregister,
    scrollbarRef,
    settings.zoom,
    horizontalScroller,
    rulerRef,
  ])

  return (
    <>
      <div
        css={{
          height: SCROLLBAR_CONTAINER_HEIGHT,
          backgroundColor: colors.structure.soot,
          marginLeft: -settings.width,
          width: settings.width,
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
              width: `${100 / (settings.zoom / 100)}%`,
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
  settings: PropTypes.shape({
    width: PropTypes.number.isRequired,
    zoom: PropTypes.number.isRequired,
  }).isRequired,
  rulerRef: PropTypes.shape({
    current: PropTypes.shape({
      offsetWidth: PropTypes.number,
      scrollWidth: PropTypes.number,
    }),
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineScrollbar
