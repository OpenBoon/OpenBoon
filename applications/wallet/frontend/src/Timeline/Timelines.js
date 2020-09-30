import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import { filterTimelines } from './helpers'

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

const TimelineTimelines = ({
  videoRef,
  length,
  timelines,
  settings,
  dispatch,
}) => {
  const filteredTimelines = filterTimelines({ timelines, settings })

  return (
    <div
      css={{
        flex: 1,
        display: 'flex',
        overflow: 'auto',
        marginLeft: -settings.modulesWidth,
        borderTop: constants.borders.regular.smoke,
      }}
    >
      <div css={{ width: settings.modulesWidth }}>
        {filteredTimelines
          .filter(({ timeline }) => {
            return settings.modules[timeline]?.isVisible !== false
          })
          .map(({ timeline, tracks }, index) => {
            const colorIndex = index % COLORS.length

            return (
              <TimelineAccordion
                key={timeline}
                moduleColor={COLORS[colorIndex]}
                timeline={timeline}
                tracks={tracks}
                dispatch={dispatch}
                isOpen={settings.modules[timeline]?.isOpen || false}
              >
                {tracks.map(({ track, hits }) => {
                  return (
                    <div key={track} css={{ display: 'flex' }}>
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
                          {track}
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
        {filteredTimelines
          .filter(({ timeline }) => {
            return settings.modules[timeline]?.isVisible !== false
          })
          .map(({ timeline, tracks }, index) => {
            const colorIndex = index % COLORS.length

            return (
              <TimelineTracks
                key={timeline}
                videoRef={videoRef}
                length={length}
                moduleColor={COLORS[colorIndex]}
                tracks={tracks}
                isOpen={settings.modules[timeline]?.isOpen || false}
              />
            )
          })}
      </div>
    </div>
  )
}

TimelineTimelines.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({}),
  }).isRequired,
  length: PropTypes.number.isRequired,
  timelines: PropTypes.arrayOf(
    PropTypes.shape({
      timeline: PropTypes.string.isRequired,
      tracks: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
    }),
  ).isRequired,
  settings: PropTypes.shape({
    modulesWidth: PropTypes.number.isRequired,
    filter: PropTypes.string.isRequired,
    modules: PropTypes.shape({}).isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineTimelines
