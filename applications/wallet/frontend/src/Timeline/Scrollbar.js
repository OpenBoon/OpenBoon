import PropTypes from 'prop-types'
import { useEffect, useRef } from 'react'

import { colors, constants, spacing, zIndex } from '../Styles'

import { getScroller } from '../Scroll/helpers'

export const SCROLLBAR_CONTAINER_HEIGHT = 36

const TimelineScrollbar = ({ settings }) => {
  const horizontalScroller = getScroller({ namespace: 'Timeline' })
  const scrollbarRef = useRef()

  const horizontalScrollerDeregister = horizontalScroller.register({
    eventName: 'scroll',
    callback: /* istanbul ignore next */ ({ node }) => {
      if (!scrollbarRef.current || !node) return

      // the scrollLeft value when the timeline is scrolled all the way to the end
      const maxScrollLeft = node.scrollWidth - node.offsetWidth

      // compute scrollLeft as a percentage to simplify translating to scrollbar scrollLeft
      const percentScrolled =
        maxScrollLeft === 0 ? maxScrollLeft : node.scrollLeft / maxScrollLeft

      const {
        width: scrollbarWidth,
      } = scrollbarRef.current.getBoundingClientRect()

      const scrollbarTrackWidth = scrollbarWidth * (settings.zoom / 100)

      // the amount of space the scrollbar thumb can travel
      const scrollbarScrollableWidth = scrollbarTrackWidth - scrollbarWidth

      scrollbarRef.current.style.left = `${
        percentScrolled * scrollbarScrollableWidth
      }px`
    },
  })

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
              position: 'absolute',
              width: `${100 / (settings.zoom / 100)}%`,
              height: '100%',
              backgroundColor: colors.structure.smoke,
              borderRadius: constants.borderRadius.medium,
            }}
          />
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
}

export default TimelineScrollbar
