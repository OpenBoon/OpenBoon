import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import { filterDetections } from './helpers'

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

const TimelineDetections = ({
  videoRef,
  length,
  detections,
  settings,
  dispatch,
}) => {
  const filteredDetections = filterDetections({ detections, settings })

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
        {filteredDetections
          .filter(({ name }) => settings.modules[name]?.isVisible !== false)
          .map(({ name, predictions }, index) => {
            const colorIndex = index % COLORS.length

            return (
              <TimelineAccordion
                key={name}
                moduleColor={COLORS[colorIndex]}
                name={name}
                predictions={predictions}
                dispatch={dispatch}
                isOpen={settings.modules[name]?.isOpen || false}
              >
                {predictions.map(({ label, hits }) => {
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
                        >{`(${hits.length})`}</div>
                      </div>
                    </div>
                  )
                })}
              </TimelineAccordion>
            )
          })}
      </div>

      <div css={{ flex: 1 }}>
        {filteredDetections
          .filter(({ name }) => settings.modules[name]?.isVisible !== false)
          .map(({ name, predictions }, index) => {
            const colorIndex = index % COLORS.length

            return (
              <TimelineTracks
                key={name}
                videoRef={videoRef}
                length={length}
                moduleColor={COLORS[colorIndex]}
                predictions={predictions}
                isOpen={settings.modules[name]?.isOpen || false}
              />
            )
          })}
      </div>
    </div>
  )
}

TimelineDetections.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({}),
  }).isRequired,
  length: PropTypes.number.isRequired,
  detections: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  settings: PropTypes.shape({
    filter: PropTypes.string.isRequired,
    modules: PropTypes.shape({}).isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineDetections
