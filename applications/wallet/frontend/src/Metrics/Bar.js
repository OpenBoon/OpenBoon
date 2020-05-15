import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'
import ClockSvg from '../Icons/clock.svg'

const PIPELINE_COLORS = [
  colors.signal.sky.base,
  colors.signal.canary.strong,
  colors.signal.grass.base,
  colors.signal.canary.base,
  colors.signal.sky.base,
  colors.signal.canary.strong,
  colors.signal.grass.base,
  colors.signal.canary.base,
]

const CONTAINER_HEIGHT = 9
const BORDER_HEIGHT = 12
const MIN_WIDTH = 8

const MetricsBar = ({ pipeline }) => {
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
          return (
            <div
              key={processor.processor}
              role="button"
              tabIndex="0"
              aria-label={processor.processor}
              onKeyPress={() =>
                setLegend(
                  Object.keys(legend).length > 0
                    ? {}
                    : {
                        executionTime: processor.executionTime || 0,
                        processor: processor.processor,
                        color: PIPELINE_COLORS[index],
                      },
                )
              }
              onMouseEnter={() =>
                setLegend({
                  executionTime: processor.executionTime || 0,
                  processor: processor.processor,
                  color: PIPELINE_COLORS[index],
                })
              }
              onMouseLeave={() => setLegend({})}
              css={{
                height: '100%',
                flex: `${processor.executionTime} 0 auto`,
                minWidth: MIN_WIDTH,
                backgroundColor: PIPELINE_COLORS[index],
                ':hover:before': {
                  content: `''`,
                  display: 'block',
                  position: 'relative',
                  height: BORDER_HEIGHT,
                  top: -1,
                  border: constants.borders.metrics,
                },
                '&:first-of-type': {
                  borderTopLeftRadius: constants.borderRadius.small,
                  borderBottomLeftRadius: constants.borderRadius.small,
                  ':hover:before': {
                    borderTopLeftRadius: constants.borderRadius.small,
                    borderBottomLeftRadius: constants.borderRadius.small,
                  },
                },
                '&:last-of-type': {
                  borderTopRightRadius: constants.borderRadius.small,
                  borderBottomRightRadius: constants.borderRadius.small,
                  ':hover:before': {
                    borderTopRightRadius: constants.borderRadius.small,
                    borderBottomRightRadius: constants.borderRadius.small,
                  },
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
              paddingLeft: spacing.small,
              paddingRight: spacing.small,
            }}
          >
            <div
              css={{
                display: 'flex',
                alignItems: 'center',
                padding: spacing.base,
              }}
            >
              <ClockSvg width={20} />
              <div
                css={{ paddingLeft: spacing.base, paddingRight: spacing.base }}
              >
                {legend.executionTime}
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

MetricsBar.propTypes = {
  pipeline: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
}

export default MetricsBar
