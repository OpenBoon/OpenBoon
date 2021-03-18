import PropTypes from 'prop-types'
import { useEffect, useRef } from 'react'

import { colors, constants, zIndex } from '../Styles'

import { getScroller } from '../Scroll/helpers'

export const LAST_ROW_HEIGHT = 36
const SCROLLBAR_HEIGHT = 12

const TimelineScrollbar = ({ settings }) => {
  const horizontalScroller = getScroller({ namespace: 'Timeline' })
  const scrollbarRef = useRef()

  useEffect(() => {
    const horizontalScrollerDeregister = horizontalScroller.register({
      eventName: 'scroll',
      callback: /* istanbul ignore next */ ({ node }) => {
        if (!scrollbarRef.current || !node) return

        scrollbarRef.current.style.left = `${
          node.scrollLeft / (settings.zoom / 100)
        }px`
      },
    })

    return () => {
      horizontalScrollerDeregister()
    }
  }, [horizontalScroller, scrollbarRef, settings.zoom])

  return (
    <>
      <div
        css={{
          height: LAST_ROW_HEIGHT,
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
          height: LAST_ROW_HEIGHT,
          backgroundColor: colors.structure.soot,
          zIndex: zIndex.timeline.tracks + 1,
        }}
      >
        <div
          css={{
            position: 'relative',
            width: '100%',
            height: SCROLLBAR_HEIGHT,
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
              height: SCROLLBAR_HEIGHT - 2,
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
