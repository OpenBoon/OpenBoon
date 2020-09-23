import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import TimelineAccordion, { COLOR_TAB_WIDTH } from './Accordion'
import TimelineTracks from './Tracks'

const COLORS = [
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

const TimelineDetections = ({ detections, settings, dispatch }) => {
  return (
    <div
      css={{
        flex: 1,
        display: 'flex',
        overflow: 'auto',
        marginLeft: -constants.timeline.modulesWidth,
        borderTop: constants.borders.regular.smoke,
      }}
    >
      <div css={{ width: constants.timeline.modulesWidth }}>
        {detections
          .filter(({ name }) => settings[name]?.isVisible !== false)
          .map(({ name, predictions }, index) => {
            const colorIndex = index % COLORS.length

            return (
              <TimelineAccordion
                key={name}
                moduleColor={COLORS[colorIndex]}
                name={name}
                predictions={predictions}
                dispatch={dispatch}
                isOpen={settings[name]?.isOpen || false}
              >
                {predictions.map(({ label, count }) => {
                  return (
                    <div key={label} css={{ display: 'flex' }}>
                      <div
                        css={{
                          width: COLOR_TAB_WIDTH,
                          backgroundColor: COLORS[colorIndex],
                        }}
                      />
                      <div
                        css={{
                          display: 'flex',
                          width: '100%',
                          borderTop: constants.borders.regular.smoke,
                          backgroundColor: colors.structure.coal,
                          overflow: 'hidden',
                        }}
                      >
                        <div
                          css={{
                            flex: 1,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            padding: spacing.base,
                            paddingLeft: spacing.base + spacing.spacious,
                            paddingRight: 0,
                          }}
                        >
                          {label}
                        </div>
                        <div
                          css={{ padding: spacing.base }}
                        >{`(${count})`}</div>
                      </div>
                    </div>
                  )
                })}
              </TimelineAccordion>
            )
          })}
      </div>

      <div css={{ flex: 1 }}>
        {detections
          .filter(({ name }) => settings[name]?.isVisible !== false)
          .map(({ name, predictions }) => {
            return (
              <TimelineTracks
                key={name}
                name={name}
                predictions={predictions}
                isOpen={settings[name]?.isOpen || false}
              />
            )
          })}
      </div>
    </div>
  )
}

TimelineDetections.propTypes = {
  detections: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  settings: PropTypes.shape({}).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineDetections
