/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'
import AutoSizer from 'react-virtualized-auto-sizer'

import { colors, constants, spacing, typography } from '../Styles'

import {
  formatPaddedSeconds,
  getRulerLayout,
  MAJOR_TICK_HEIGHT,
  MINOR_TICK_HEIGHT,
  TICK_WIDTH,
  MIN_TICK_SPACING,
} from './helpers'

const OFFSET = (TICK_WIDTH + constants.borderWidths.regular) / 2

const TimelineRuler = ({
  videoRef,
  rulerRef,
  length,
  width: settingsWidth,
}) => {
  return (
    <AutoSizer defaultWidth={500} disableHeight>
      {({ width }) => {
        const { halfSeconds, majorStep } = getRulerLayout({ length, width })

        return (
          <div
            onClick={({ clientX }) => {
              videoRef.current.pause()

              const newPosition =
                clientX - settingsWidth + rulerRef.current.scrollLeft

              const newCurrentTime =
                (newPosition / width) * videoRef.current.duration

              // eslint-disable-next-line no-param-reassign
              videoRef.current.currentTime = newCurrentTime
            }}
            css={{
              display: 'flex',
              alignItems: 'flex-end',
              height: constants.timeline.rulerRowHeight,
              width: width + OFFSET,
              marginLeft: -OFFSET,
              backgroundColor: colors.structure.lead,
              cursor: 'pointer',
            }}
          >
            {halfSeconds.map((halfSecond) => {
              const isMajor = halfSecond % majorStep === 0
              const label = halfSecond / 2

              if (halfSecond % (majorStep / 2) !== 0) return null

              const isLabelSpaceAvailable =
                width - (label / length) * width > MIN_TICK_SPACING

              /**
               * account for tick width and width of border
               * between TimelineRuler and TimelineAggregate
               */
              const leftOffset = `calc(${
                (label / length) * 100
              }% - ${OFFSET}px)`

              return (
                <div
                  key={halfSecond}
                  css={{
                    position: 'absolute',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    fontFamily: typography.family.condensed,
                    color: colors.structure.steel,
                    left: leftOffset,
                  }}
                >
                  {halfSecond !== 0 && isMajor && isLabelSpaceAvailable && (
                    <div
                      css={{
                        position: 'absolute',
                        top: -(MAJOR_TICK_HEIGHT + spacing.mini),
                        userSelect: 'none',
                      }}
                    >
                      {formatPaddedSeconds({ seconds: label })}
                    </div>
                  )}
                  <div
                    css={{
                      width: TICK_WIDTH,
                      height: isMajor ? MAJOR_TICK_HEIGHT : MINOR_TICK_HEIGHT,
                      backgroundColor: colors.structure.iron,
                    }}
                  />
                </div>
              )
            })}
          </div>
        )
      }}
    </AutoSizer>
  )
}

TimelineRuler.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      pause: PropTypes.func,
      currentTime: PropTypes.number,
      duration: PropTypes.number,
    }),
  }).isRequired,
  rulerRef: PropTypes.shape({
    current: PropTypes.shape({
      scrollLeft: PropTypes.number.isRequired,
    }),
  }).isRequired,
  length: PropTypes.number.isRequired,
  width: PropTypes.number.isRequired,
}

export default TimelineRuler
