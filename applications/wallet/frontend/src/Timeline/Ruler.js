import PropTypes from 'prop-types'
import AutoSizer from 'react-virtualized-auto-sizer'

import { colors, constants, typography } from '../Styles'

import {
  formatPaddedSeconds,
  getRulerLayout,
  MAJOR_TICK_HEIGHT,
  MINOR_TICK_HEIGHT,
  TICK_WIDTH,
  MIN_TICK_SPACING,
} from './helpers'

const OFFSET = (TICK_WIDTH + constants.borderWidths.regular) / 2

const TimelineRuler = ({ length }) => {
  return (
    <AutoSizer defaultWidth={500} disableHeight>
      {({ width }) => {
        const { halfSeconds, majorStep } = getRulerLayout({ length, width })

        return (
          <div
            css={{
              display: 'flex',
              alignItems: 'flex-end',
              height: constants.timeline.rulerRowHeight,
              width: width + OFFSET,
              marginLeft: -OFFSET,
              backgroundColor: colors.structure.lead,
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
                      css={{ position: 'absolute', top: -MAJOR_TICK_HEIGHT }}
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
  length: PropTypes.number.isRequired,
}

export default TimelineRuler
