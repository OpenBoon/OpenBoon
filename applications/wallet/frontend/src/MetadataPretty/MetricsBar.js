import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'
import ClockSvg from '../Icons/clock.svg'

const PIPELINE_COLORS = [
  colors.signal.sky.base,
  colors.graph.magenta,
  colors.signal.halloween.base,
  colors.signal.canary.base,
  colors.graph.seafoam,
  colors.graph.rust,
  colors.graph.coral,
  colors.graph.iris,
  colors.graph.marigold,
  colors.graph.magenta,
  colors.signal.grass.base,
]

const CONTAINER_HEIGHT = 9
const HOVER_HEIGHT = 13
const MIN_WIDTH = 8

const MetadataPrettyMetricsBar = ({ pipeline }) => {
  const [legend, setLegend] = useState({})

  return (
    <div
      css={{
        display: 'flex',
        height: CONTAINER_HEIGHT,
        position: 'relative',
      }}
    >
      <div css={{ display: 'flex', width: '100%' }}>
        {pipeline.map((processor, index) => {
          const colorIndex = index % PIPELINE_COLORS.length

          return (
            <button
              key={processor.processor}
              aria-label={processor.processor}
              type="button"
              onFocus={() =>
                setLegend({
                  executionTime: processor.executionTime,
                  processor: processor.processor,
                  color: PIPELINE_COLORS[colorIndex],
                })
              }
              onBlur={() => {
                setLegend({})
              }}
              onMouseEnter={() =>
                setLegend({
                  executionTime: processor.executionTime,
                  processor: processor.processor,
                  color: PIPELINE_COLORS[index],
                })
              }
              onMouseLeave={() => setLegend({})}
              css={{
                height: '100%',
                flex: `${processor.executionTime} 0 auto`,
                minWidth: MIN_WIDTH,
                backgroundColor: PIPELINE_COLORS[colorIndex],
                border: '2px solid transparent',
                ':hover, :focus': {
                  position: 'relative',
                  height: HOVER_HEIGHT,
                  top: -2,
                  border: constants.borders.metrics,
                  outline: 0,
                },
                '&:first-of-type': {
                  borderTopLeftRadius: constants.borderRadius.small,
                  borderBottomLeftRadius: constants.borderRadius.small,
                },
                '&:last-of-type': {
                  borderTopRightRadius: constants.borderRadius.small,
                  borderBottomRightRadius: constants.borderRadius.small,
                },
              }}
            />
          )
        })}
      </div>
      {Object.keys(legend).length > 0 && (
        <div
          css={{
            position: 'absolute',
            top: CONTAINER_HEIGHT + spacing.base,
            right: 0,
            boxShadow: constants.boxShadows.tableRow,
            zIndex: zIndex.reset,
            width: '100%',
          }}
        >
          <div
            css={{
              display: 'flex',
              backgroundColor: legend.color,
              borderRadius: constants.borderRadius.small,
            }}
          >
            <div
              css={{
                display: 'flex',
                alignItems: 'center',
                padding: spacing.base,
                width: '100%',
              }}
            >
              <div css={{ display: 'flex', alignItems: 'center' }}>
                <ClockSvg width={20} />
                <div
                  css={{
                    paddingLeft: spacing.base,
                    paddingRight: spacing.base,
                  }}
                >
                  {legend.executionTime}
                </div>
              </div>
              <div
                css={{
                  width: 1,
                  height: '100%',
                  backgroundColor: colors.structure.white,
                  opacity: '50%',
                }}
              />
              <div
                css={{
                  paddingLeft: spacing.base,
                  fontFamily: 'Roboto Condensed',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {legend.processor}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

MetadataPrettyMetricsBar.propTypes = {
  pipeline: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
}

export default MetadataPrettyMetricsBar
