import PropTypes from 'prop-types'
import { useMemo, useRef, useEffect } from 'react'

import { colors, constants, spacing, zIndex } from '../Styles'

import { getScroller } from '../Scroll/helpers'

import TimelineScrollbarThumb from './ScrollbarThumb'
import TimelineScrollbarHandle from './ScrollbarHandle'

import { SCROLLBAR_CONTAINER_HEIGHT, getIgnore } from './helpers'

const TimelineScrollbar = ({ rulerRef, width, initialZoom, dispatch }) => {
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const zoom = useMemo(() => initialZoom, [])

  const scrollbarTrackRef = useRef()
  const scrollbarRef = useRef()

  const horizontalScroller = getScroller({ namespace: 'Timeline' })

  const horizontalScrollerDeregister = horizontalScroller.register({
    eventName: 'scroll',
    callback: /* istanbul ignore next */ ({
      node: { scrollLeft, scrollWidth } = {},
    }) => {
      const ignore = getIgnore()

      if (ignore || !scrollbarRef.current || !scrollLeft || !scrollWidth) return

      /* eslint-disable no-param-reassign */
      scrollbarRef.current.style.left = `${(scrollLeft / scrollWidth) * 100}%`
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
              width: `${(100 / zoom) * 100}%`,
              height: '100%',
              backgroundColor: colors.structure.smoke,
              borderRadius: constants.borderRadius.medium,
            }}
          >
            <TimelineScrollbarHandle
              scrollbarRef={scrollbarRef}
              scrollbarTrackRef={scrollbarTrackRef}
              horizontalScroller={horizontalScroller}
              isLeft
              dispatch={dispatch}
            />
            <TimelineScrollbarThumb
              rulerRef={rulerRef}
              scrollbarRef={scrollbarRef}
              scrollbarTrackRef={scrollbarTrackRef}
              horizontalScroller={horizontalScroller}
            />
            <TimelineScrollbarHandle
              scrollbarRef={scrollbarRef}
              scrollbarTrackRef={scrollbarTrackRef}
              horizontalScroller={horizontalScroller}
              isLeft={false}
              dispatch={dispatch}
            />
          </div>
        </div>
      </div>
    </>
  )
}

TimelineScrollbar.propTypes = {
  rulerRef: PropTypes.shape({
    current: PropTypes.shape({
      offsetWidth: PropTypes.number,
      scrollWidth: PropTypes.number,
    }),
  }).isRequired,
  width: PropTypes.number.isRequired,
  initialZoom: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineScrollbar
