import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import { filterTimelines } from './helpers'

import TimelineTracks from './Tracks'

const TimelineSearchHits = ({ videoRef, length, timelines, settings }) => {
  const filteredTimelines = filterTimelines({ timelines, settings })

  const aggregate = {}

  filteredTimelines.forEach(({ tracks }) => {
    tracks.forEach(({ hits }) => {
      hits.forEach(({ start, stop, highlight }) => {
        if (!aggregate[start] && highlight === true) {
          aggregate[start] = { start, stop, highlight }
        }
      })
    })
  })

  return (
    <div
      css={{
        display: 'flex',
        overflow: 'overlay',
        marginLeft: -settings.width,
        borderTop: constants.borders.regular.smoke,
        backgroundColor: colors.structure.soot,
      }}
    >
      <div
        css={{
          width: settings.width,
          padding: spacing.base,
          paddingLeft: spacing.normal,
        }}
      >
        Search Hits ({Object.keys(aggregate).length})
      </div>

      <div
        css={{
          flex: 1,
          overflowY: 'hidden',
          overflowX: 'overlay',
        }}
      >
        <div css={{ width: `${settings.zoom}%` }}>
          <TimelineTracks
            videoRef={videoRef}
            length={length}
            color={colors.structure.white}
            tracks={[{ track: 'highlights', hits: Object.values(aggregate) }]}
            isOpen={false}
          />
        </div>
      </div>
    </div>
  )
}

TimelineSearchHits.propTypes = {
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
    width: PropTypes.number.isRequired,
    filter: PropTypes.string.isRequired,
    timelines: PropTypes.shape({}).isRequired,
    zoom: PropTypes.number.isRequired,
  }).isRequired,
}

export default TimelineSearchHits