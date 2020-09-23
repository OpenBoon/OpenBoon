import AutoSizer from 'react-virtualized-auto-sizer'

import { colors, constants, spacing } from '../Styles'

const MAJOR_HEIGHT = 16
const MINOR_HEIGHT = 12
const TICK_WIDTH = 2

const TimelineRuler = () => {
  return (
    <AutoSizer defaultWidth={64} disableHeight>
      {({ width }) => {
        const numTicks = Math.floor(width / 32)

        const ticks = Array.from({ length: numTicks }, (x, i) => i)

        return (
          <div
            css={{
              display: 'flex',
              alignItems: 'flex-end',
              height: constants.timeline.rulerRowHeight,
              width: 'fit-content',
              marginLeft: -(TICK_WIDTH + constants.borderWidths.regular) / 2,
            }}
          >
            {ticks.map((tick) => {
              return (
                <div
                  key={tick}
                  css={{
                    width: TICK_WIDTH,
                    height: tick % 2 === 0 ? MAJOR_HEIGHT : MINOR_HEIGHT,
                    backgroundColor: colors.structure.steel,
                    marginRight: spacing.spacious,
                  }}
                />
              )
            })}
          </div>
        )
      }}
    </AutoSizer>
  )
}

export default TimelineRuler
