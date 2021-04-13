import PropTypes from 'prop-types'
import { useRef } from 'react'

import { colors, constants, spacing, zIndex } from '../Styles'

import TimelineScrollbarThumb from './ScrollbarThumb'
import TimelineScrollbarRightHandle from './ScrollbarRightHandle'

export const SCROLLBAR_CONTAINER_HEIGHT = 36
const RESIZE_HANDLE_SIZE = 20

const TimelineScrollbar = ({ width, zoom, rulerRef }) => {
  const scrollbarTrackRef = useRef()
  const scrollbarRef = useRef()

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
              width: '100%',
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
            <TimelineScrollbarThumb
              scrollbarRef={scrollbarRef}
              zoom={zoom}
              rulerRef={rulerRef}
            />
            <TimelineScrollbarRightHandle
              scrollbarRef={scrollbarRef}
              scrollbarTrackRef={scrollbarTrackRef}
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
